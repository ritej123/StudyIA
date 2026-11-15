package com.studyai.dto.request;

import com.studyai.entity.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Requête de modification du statut d'un utilisateur par l'administrateur.
 */
@Data
public class UpdateUserStatusRequest {

    @NotNull(message = "Le statut est obligatoire")
    private UserStatus status;
}
