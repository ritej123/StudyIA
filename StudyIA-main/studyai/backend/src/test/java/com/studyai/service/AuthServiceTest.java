package com.studyai.service;

import com.studyai.config.RateLimitConfig;
import com.studyai.dto.request.LoginRequest;
import com.studyai.dto.request.RegisterRequest;
import com.studyai.dto.response.AuthResponse;
import com.studyai.entity.User;
import com.studyai.entity.enums.Role;
import com.studyai.entity.enums.UserStatus;
import com.studyai.exception.AccountDisabledException;
import com.studyai.exception.EmailAlreadyExistsException;
import com.studyai.exception.InvalidCredentialsException;
import com.studyai.exception.RateLimitExceededException;
import com.studyai.repository.UserRepository;
import com.studyai.security.UserDetailsServiceImpl;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenService tokenService;

    @Mock
    private RateLimitConfig rateLimitConfig;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private Bucket bucket;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private HttpServletResponse httpResponse;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationDays", 7);
    }

    @Test
    @DisplayName("Inscription avec succès d'un nouvel étudiant")
    void register_Success() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Jean");
        request.setLastName("Dupont");
        request.setEmail("jean.dupont@studyai.com");
        request.setPassword("Secret123");

        UserDetails userDetails = mock(UserDetails.class);

        when(userRepository.existsByEmail("jean.dupont@studyai.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(userDetailsService.loadUserByUsername("jean.dupont@studyai.com")).thenReturn(userDetails);
        when(jwtService.generateToken(eq(userDetails), anyMap())).thenReturn("mocked-jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(900000L);
        when(tokenService.createRefreshToken(any(User.class))).thenReturn("raw-refresh-token");

        // When
        AuthResponse response = authService.register(request, httpResponse);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mocked-jwt-token");
        assertThat(response.getEmail()).isEqualTo("jean.dupont@studyai.com");
        assertThat(response.getRole()).isEqualTo(Role.ETUDIANT);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(httpResponse).addCookie(cookieCaptor.capture());
        Cookie cookie = cookieCaptor.getValue();
        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getValue()).isEqualTo("raw-refresh-token");
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    @DisplayName("Inscription échouée car email déjà existant")
    void register_EmailAlreadyExists() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("deja.pris@studyai.com");

        when(userRepository.existsByEmail("deja.pris@studyai.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(request, httpResponse))
            .isInstanceOf(EmailAlreadyExistsException.class)
            .hasMessageContaining("Cet email existe déjà");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Connexion réussie avec identifiants valides")
    void login_Success() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("etudiant@studyai.com");
        request.setPassword("Password123");

        User user = User.builder()
            .id(2L)
            .firstName("Alice")
            .lastName("Martin")
            .email("etudiant@studyai.com")
            .passwordHash("hashedPassword")
            .role(Role.ETUDIANT)
            .status(UserStatus.ACTIF)
            .build();

        UserDetails userDetails = mock(UserDetails.class);

        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.10");
        when(rateLimitConfig.getLoginBucket("192.168.1.10")).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);

        when(userRepository.findByEmail("etudiant@studyai.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "hashedPassword")).thenReturn(true);
        when(userDetailsService.loadUserByUsername("etudiant@studyai.com")).thenReturn(userDetails);
        when(jwtService.generateToken(eq(userDetails), anyMap())).thenReturn("valid-jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(900000L);
        when(tokenService.createRefreshToken(user)).thenReturn("new-refresh-token");

        // When
        AuthResponse response = authService.login(request, httpRequest, httpResponse);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("valid-jwt-token");
        assertThat(response.getUserId()).isEqualTo(2L);
        verify(httpResponse).addCookie(any(Cookie.class));
    }

    @Test
    @DisplayName("Connexion échouée pour mauvais mot de passe")
    void login_InvalidCredentials_BadPassword() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("etudiant@studyai.com");
        request.setPassword("WrongPassword");

        User user = User.builder()
            .email("etudiant@studyai.com")
            .passwordHash("hashedPassword")
            .status(UserStatus.ACTIF)
            .build();

        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitConfig.getLoginBucket("127.0.0.1")).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);
        when(userRepository.findByEmail("etudiant@studyai.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hashedPassword")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(request, httpRequest, httpResponse))
            .isInstanceOf(InvalidCredentialsException.class)
            .hasMessage("Email ou mot de passe incorrect");
    }

    @Test
    @DisplayName("Connexion échouée pour compte désactivé")
    void login_AccountDisabled() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("bloque@studyai.com");
        request.setPassword("Password123");

        User user = User.builder()
            .email("bloque@studyai.com")
            .passwordHash("hashedPassword")
            .status(UserStatus.DESACTIVE)
            .build();

        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitConfig.getLoginBucket("127.0.0.1")).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(true);
        when(userRepository.findByEmail("bloque@studyai.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123", "hashedPassword")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.login(request, httpRequest, httpResponse))
            .isInstanceOf(AccountDisabledException.class)
            .hasMessage("Votre compte a été désactivé");
    }

    @Test
    @DisplayName("Connexion bloquée par dépassement du rate limit")
    void login_RateLimitExceeded() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("user@studyai.com");
        request.setPassword("Password123");

        when(httpRequest.getRemoteAddr()).thenReturn("10.0.0.1");
        when(rateLimitConfig.getLoginBucket("10.0.0.1")).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(request, httpRequest, httpResponse))
            .isInstanceOf(RateLimitExceededException.class)
            .hasMessage("Trop de tentatives. Veuillez réessayer dans 15 minutes");

        verify(userRepository, never()).findByEmail(anyString());
    }
}
