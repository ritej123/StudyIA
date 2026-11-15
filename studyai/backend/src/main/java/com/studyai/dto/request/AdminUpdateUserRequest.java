package com.studyai.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Requête de modification des informations d'un utilisateur par l'administrateur.
 */
@Data
public class AdminUpdateUserRequest {

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 50)
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50)
    private String lastName;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;
}
