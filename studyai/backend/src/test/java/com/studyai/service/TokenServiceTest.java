package com.studyai.service;

import com.studyai.entity.RefreshToken;
import com.studyai.entity.User;
import com.studyai.exception.InvalidRefreshTokenException;
import com.studyai.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private TokenService tokenService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenService, "refreshTokenExpirationDays", 7);
        sampleUser = User.builder()
            .id(5L)
            .email("etudiant@studyai.com")
            .build();
    }

    @Test
    @DisplayName("Création d'un refresh token avec hachage SHA-256")
    void createRefreshToken_Success() {
        // When
        String rawToken = tokenService.createRefreshToken(sampleUser);

        // Then
        assertThat(rawToken).isNotBlank();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken savedToken = captor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(sampleUser);
        assertThat(savedToken.getTokenHash()).isEqualTo(tokenService.hashToken(rawToken));
        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());
        assertThat(savedToken.isValid()).isTrue();
    }

    @Test
    @DisplayName("Rotation d'un refresh token : révocation de l'ancien et génération du nouveau")
    void rotateRefreshToken_Success() {
        // Given
        String oldRawToken = "old-raw-token-12345";
        String oldHash = tokenService.hashToken(oldRawToken);

        RefreshToken existingToken = RefreshToken.builder()
            .tokenHash(oldHash)
            .user(sampleUser)
            .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
            .createdAt(Instant.now())
            .build();

        when(refreshTokenRepository.findByTokenHash(oldHash)).thenReturn(Optional.of(existingToken));

        // When
        String newRawToken = tokenService.rotateRefreshToken(oldRawToken);

        // Then
        assertThat(newRawToken).isNotBlank();
        assertThat(newRawToken).isNotEqualTo(oldRawToken);
        assertThat(existingToken.getRevokedAt()).isNotNull();
        assertThat(existingToken.isRevoked()).isTrue();

        // 1er save pour marquer révoqué, 2nd save pour persister le nouveau token
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Rotation échouée si le token est expiré")
    void rotateRefreshToken_ExpiredToken() {
        // Given
        String rawToken = "expired-raw-token";
        String tokenHash = tokenService.hashToken(rawToken);

        RefreshToken expiredToken = RefreshToken.builder()
            .tokenHash(tokenHash)
            .user(sampleUser)
            .expiresAt(Instant.now().minus(2, ChronoUnit.HOURS))
            .createdAt(Instant.now().minus(8, ChronoUnit.DAYS))
            .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredToken));

        // When & Then
        assertThatThrownBy(() -> tokenService.rotateRefreshToken(rawToken))
            .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Rotation échouée si le token est déjà révoqué")
    void rotateRefreshToken_RevokedToken() {
        // Given
        String rawToken = "already-revoked-token";
        String tokenHash = tokenService.hashToken(rawToken);

        RefreshToken revokedToken = RefreshToken.builder()
            .tokenHash(tokenHash)
            .user(sampleUser)
            .expiresAt(Instant.now().plus(5, ChronoUnit.DAYS))
            .revokedAt(Instant.now().minus(10, ChronoUnit.MINUTES))
            .createdAt(Instant.now().minus(2, ChronoUnit.DAYS))
            .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(revokedToken));

        // When & Then
        assertThatThrownBy(() -> tokenService.rotateRefreshToken(rawToken))
            .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }
}
