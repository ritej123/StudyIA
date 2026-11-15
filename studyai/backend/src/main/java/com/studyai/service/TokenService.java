package com.studyai.service;

import com.studyai.entity.RefreshToken;
import com.studyai.entity.User;
import com.studyai.exception.InvalidRefreshTokenException;
import com.studyai.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

/**
 * Service de gestion des Refresh Tokens (création, hachage, rotation, révocation).
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.refresh-token.expiration-days}")
    private int refreshTokenExpirationDays;

    /**
     * Crée un nouveau refresh token pour l'utilisateur et le stocke en base de données.
     */
    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
            .tokenHash(tokenHash)
            .user(user)
            .expiresAt(Instant.now().plus(refreshTokenExpirationDays, ChronoUnit.DAYS))
            .createdAt(Instant.now())
            .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    /**
     * Rotation : révoque l'ancien token et crée un nouveau token.
     */
    @Transactional
    public String rotateRefreshToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(InvalidRefreshTokenException::new);

        if (!existing.isValid()) {
            throw new InvalidRefreshTokenException();
        }

        // Révoquer l'ancien
        existing.setRevokedAt(Instant.now());
        refreshTokenRepository.save(existing);

        // Créer un nouveau
        return createRefreshToken(existing.getUser());
    }

    /**
     * Récupère l'utilisateur associé au refresh token.
     */
    @Transactional(readOnly = true)
    public User getUserFromRefreshToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(InvalidRefreshTokenException::new);

        if (!refreshToken.isValid()) {
            throw new InvalidRefreshTokenException();
        }
        return refreshToken.getUser();
    }

    /**
     * Révoque tous les refresh tokens actifs d'un utilisateur.
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
    }

    /**
     * Hash SHA-256 du token brut pour stockage sécurisé.
     */
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erreur de hashage du token", e);
        }
    }
}
