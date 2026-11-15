package com.studyai.dto.response;

import com.studyai.entity.enums.Role;
import com.studyai.entity.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Réponse contenant les informations du profil utilisateur connecté.
 */
@Data
@Builder
public class UserProfileResponse {
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
