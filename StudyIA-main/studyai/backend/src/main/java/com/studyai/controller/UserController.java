package com.studyai.controller;

import com.studyai.dto.request.ChangePasswordRequest;
import com.studyai.dto.request.RegisterRequest;
import com.studyai.dto.request.UpdateProfileRequest;
import com.studyai.dto.response.AuthResponse;
import com.studyai.dto.response.MessageResponse;
import com.studyai.dto.response.UserProfileResponse;
import com.studyai.service.AuthService;
import com.studyai.service.StorageService;
import com.studyai.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contrôleur de gestion des utilisateurs et du profil personnel des étudiants.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Endpoints de gestion du profil étudiant")
public class UserController {

    private final AuthService authService;
    private final UserService userService;
    private final StorageService storageService;

    @PostMapping("/api/users/register")
    @Operation(summary = "Inscription d'un nouvel étudiant")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.register(request, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @GetMapping("/api/users/profile")
    @Operation(summary = "Voir mon profil", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getProfile(userDetails.getUsername()));
    }

    @PutMapping("/api/users/profile")
    @Operation(summary = "Modifier mon profil", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userDetails.getUsername(), request));
    }

    @PostMapping("/api/users/profile/photo")
    @Operation(summary = "Upload de la photo de profil", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserProfileResponse> uploadPhoto(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userService.uploadPhoto(userDetails.getUsername(), file));
    }

    @GetMapping("/api/users/profile/photo/{filename}")
    @Operation(summary = "Télécharger une photo de profil", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<byte[]> getPhoto(
            @PathVariable String filename,
            @AuthenticationPrincipal UserDetails userDetails) {
        byte[] data = storageService.load(filename);
        MediaType mediaType = filename.endsWith(".png") ? MediaType.IMAGE_PNG :
                              filename.endsWith(".webp") ? MediaType.parseMediaType("image/webp") :
                              MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(mediaType).body(data);
    }

    @PutMapping("/api/users/password")
    @Operation(summary = "Changer mon mot de passe", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(new MessageResponse("Mot de passe mis à jour avec succès"));
    }
}
