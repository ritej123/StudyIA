package com.studyai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entité représentant un jeton de rafraîchissement (Refresh Token).
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Vérifie si le token a été révoqué.
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Vérifie si le token a expiré.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Vérifie si le token est encore valide.
     */
    public boolean isValid() {
        return !isRevoked() && !isExpired();
    }
}
