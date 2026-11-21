package com.studyai.service;

import com.studyai.config.RateLimitConfig;
import com.studyai.dto.request.LoginRequest;
import com.studyai.dto.request.RegisterRequest;
import com.studyai.dto.response.AuthResponse;
import com.studyai.entity.User;
import com.studyai.entity.enums.Role;
import com.studyai.entity.enums.UserStatus;
import com.studyai.exception.*;
import com.studyai.repository.UserRepository;
import com.studyai.security.UserDetailsServiceImpl;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;

/**
 * Service gérant l'authentification, l'enregistrement, la déconnexion et le rafraîchissement des tokens.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final RateLimitConfig rateLimitConfig;
    private final UserDetailsServiceImpl userDetailsService;

    @Value("${app.refresh-token.expiration-days}")
    private int refreshTokenExpirationDays;

    /**
     * Inscription d'un nouvel étudiant.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
            .firstName(request.getFirstName().trim())
            .lastName(request.getLastName().trim())
            .email(request.getEmail().toLowerCase().trim())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(Role.ETUDIANT)
            .status(UserStatus.ACTIF)
            .build();

        user = userRepository.save(user);
        log.info("Nouvel étudiant inscrit : {}", user.getEmail());

        return buildAuthResponse(user, response);
    }

    /**
     * Connexion d'un utilisateur avec limitation du taux de requêtes par IP.
     */
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        String clientIp = getClientIp(httpRequest);
        Bucket bucket = rateLimitConfig.getLoginBucket(clientIp);

        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException();
        }

        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (user.getStatus() == UserStatus.DESACTIVE) {
            throw new AccountDisabledException();
        }

        if (user.getStatus() == UserStatus.EN_ATTENTE) {
            throw new AccountPendingException();
        }

        log.info("Connexion réussie : {}", user.getEmail());
        return buildAuthResponse(user, response);
    }

    /**
     * Rafraîchissement de l'access token via le refresh token stocké en cookie HTTP-Only.
     */
    @Transactional
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = extractRefreshTokenFromCookie(request);
        if (rawRefreshToken == null) throw new InvalidRefreshTokenException();

        User user = tokenService.getUserFromRefreshToken(rawRefreshToken);

        if (user.getStatus() == UserStatus.DESACTIVE) {
            tokenService.revokeAllUserTokens(user.getId());
            clearRefreshCookie(response);
            throw new AccountDisabledException();
        }

        // Rotation du refresh token
        String newRawToken = tokenService.rotateRefreshToken(rawRefreshToken);
        setRefreshCookie(response, newRawToken);

        var userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails, Map.of("role", user.getRole().name()));

        return AuthResponse.builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .expiresIn(jwtService.getExpirationMs() / 1000)
            .userId(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .role(user.getRole())
            .photoUrl(user.getPhotoUrl())
            .build();
    }

    /**
     * Déconnexion : révoque le refresh token et supprime le cookie.
     */
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = extractRefreshTokenFromCookie(request);
        if (rawRefreshToken != null) {
            try {
                User user = tokenService.getUserFromRefreshToken(rawRefreshToken);
                tokenService.revokeAllUserTokens(user.getId());
            } catch (Exception ignored) {}
        }
        clearRefreshCookie(response);
    }

    /**
     * Construit la réponse d'authentification et pose le cookie refresh.
     */
    private AuthResponse buildAuthResponse(User user, HttpServletResponse response) {
        var userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails, Map.of("role", user.getRole().name()));
        String refreshToken = tokenService.createRefreshToken(user);
        setRefreshCookie(response, refreshToken);

        return AuthResponse.builder()
            .accessToken(accessToken)
            .tokenType("Bearer")
            .expiresIn(jwtService.getExpirationMs() / 1000)
            .userId(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .role(user.getRole())
            .photoUrl(user.getPhotoUrl())
            .build();
    }

    /**
     * Pose le cookie HttpOnly du refresh token.
     */
    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(refreshTokenExpirationDays * 24 * 3600);
        // cookie.setSecure(true); // Décommenter en production
        response.addCookie(cookie);
    }

    /**
     * Supprime le cookie du refresh token.
     */
    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /**
     * Extrait le refresh token du cookie de la requête HTTP.
     */
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
            .filter(c -> "refresh_token".equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .filter(v -> !v.isBlank())
            .orElse(null);
    }

    /**
     * Récupère l'adresse IP du client en gérant les proxys inverses.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
