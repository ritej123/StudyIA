package com.studyai.controller;

import com.studyai.dto.request.AdminUpdateUserRequest;
import com.studyai.dto.request.UpdateUserStatusRequest;
import com.studyai.dto.response.AdminStatsResponse;
import com.studyai.dto.response.AdminUserResponse;
import com.studyai.dto.response.MessageResponse;
import com.studyai.dto.response.PagedResponse;
import com.studyai.service.AdminService;
import com.studyai.service.AdminStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur d'administration pour la gestion des utilisateurs par les administrateurs.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration", description = "Endpoints de gestion des comptes (Admin)")
public class AdminController {

    private final AdminService adminService;
    private final AdminStatsService adminStatsService;

    @GetMapping("/stats")
    @Operation(summary = "Statistiques d'utilisation de la plateforme", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminStatsService.getStats());
    }

    @GetMapping("/users")
    @Operation(summary = "Liste des étudiants", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PagedResponse<AdminUserResponse>> getStudents(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getStudents(search, page, size));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Modifier un compte étudiant", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AdminUserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        return ResponseEntity.ok(adminService.updateUser(id, request));
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "Modifier le statut d'un compte", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AdminUserResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(adminService.updateUserStatus(id, request));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Supprimer un compte étudiant", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(new MessageResponse("Compte supprimé avec succès"));
    }
}
