package com.studyai.dto.response;

import com.studyai.entity.enums.Role;
import com.studyai.entity.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Réponse contenant les détails d'un utilisateur pour la vue administrateur.
 */
@Data
@Builder
public class AdminUserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private UserStatus status;
    private String photoUrl;
    private Instant createdAt;
    private Instant updatedAt;
}
