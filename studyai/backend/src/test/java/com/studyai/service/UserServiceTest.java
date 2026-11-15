package com.studyai.service;

import com.studyai.dto.request.ChangePasswordRequest;
import com.studyai.dto.request.UpdateProfileRequest;
import com.studyai.dto.response.UserProfileResponse;
import com.studyai.entity.User;
import com.studyai.entity.enums.Role;
import com.studyai.entity.enums.UserStatus;
import com.studyai.exception.InvalidPasswordException;
import com.studyai.exception.UnsupportedFileTypeException;
import com.studyai.mapper.UserMapper;
import com.studyai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
            .id(1L)
            .firstName("Lucas")
            .lastName("Bernard")
            .email("lucas@studyai.com")
            .passwordHash("hashedPassword123")
            .role(Role.ETUDIANT)
            .status(UserStatus.ACTIF)
            .photoUrl("avatar-old.jpg")
            .build();
    }

    @Test
    @DisplayName("Récupération du profil d'un utilisateur")
    void getProfile_Success() {
        // Given
        UserProfileResponse profileResponse = UserProfileResponse.builder()
            .id(1L)
            .firstName("Lucas")
            .lastName("Bernard")
            .email("lucas@studyai.com")
            .role(Role.ETUDIANT)
            .status(UserStatus.ACTIF)
            .build();

        when(userRepository.findByEmail("lucas@studyai.com")).thenReturn(Optional.of(sampleUser));
        when(userMapper.toProfileResponse(sampleUser)).thenReturn(profileResponse);

        // When
        UserProfileResponse result = userService.getProfile("lucas@studyai.com");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("lucas@studyai.com");
        verify(userRepository).findByEmail("lucas@studyai.com");
    }

    @Test
    @DisplayName("Mise à jour du profil utilisateur")
    void updateProfile_Success() {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Lucas Modifie");
        request.setLastName("Bernard Modifie");

        when(userRepository.findByEmail("lucas@studyai.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse updatedProfile = UserProfileResponse.builder()
            .id(1L)
            .firstName("Lucas Modifie")
            .lastName("Bernard Modifie")
            .email("lucas@studyai.com")
            .build();
        when(userMapper.toProfileResponse(sampleUser)).thenReturn(updatedProfile);

        // When
        UserProfileResponse result = userService.updateProfile("lucas@studyai.com", request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Lucas Modifie");
        assertThat(sampleUser.getFirstName()).isEqualTo("Lucas Modifie");
        assertThat(sampleUser.getLastName()).isEqualTo("Bernard Modifie");
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Upload réussi d'une photo de profil avec suppression de l'ancienne")
    void uploadPhoto_Success() {
        // Given
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(userRepository.findByEmail("lucas@studyai.com")).thenReturn(Optional.of(sampleUser));
        when(storageService.store(multipartFile)).thenReturn("new-avatar.png");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = UserProfileResponse.builder()
            .photoUrl("new-avatar.png")
            .build();
        when(userMapper.toProfileResponse(sampleUser)).thenReturn(response);

        // When
        UserProfileResponse result = userService.uploadPhoto("lucas@studyai.com", multipartFile);

        // Then
        assertThat(result).isNotNull();
        assertThat(sampleUser.getPhotoUrl()).isEqualTo("new-avatar.png");
        verify(storageService).delete("avatar-old.jpg");
        verify(storageService).store(multipartFile);
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Upload photo échoue si le format de fichier n'est pas supporté")
    void uploadPhoto_UnsupportedFileType() {
        // Given
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(userRepository.findByEmail("lucas@studyai.com")).thenReturn(Optional.of(sampleUser));
        when(storageService.store(multipartFile)).thenThrow(new UnsupportedFileTypeException());

        // When & Then
        assertThatThrownBy(() -> userService.uploadPhoto("lucas@studyai.com", multipartFile))
            .isInstanceOf(UnsupportedFileTypeException.class)
            .hasMessageContaining("Type de fichier non supporté");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Modification réussie du mot de passe")
    void changePassword_Success() {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass123");
        request.setNewPassword("NewPass456");

        when(userRepository.findByEmail("lucas@studyai.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("OldPass123", "hashedPassword123")).thenReturn(true);
        when(passwordEncoder.encode("NewPass456")).thenReturn("newHashedPassword456");

        // When
        userService.changePassword("lucas@studyai.com", request);

        // Then
        assertThat(sampleUser.getPasswordHash()).isEqualTo("newHashedPassword456");
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Changement de mot de passe échoué car ancien mot de passe erroné")
    void changePassword_InvalidOldPassword() {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("FauxMotDePasse");
        request.setNewPassword("NewPass456");

        when(userRepository.findByEmail("lucas@studyai.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("FauxMotDePasse", "hashedPassword123")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.changePassword("lucas@studyai.com", request))
            .isInstanceOf(InvalidPasswordException.class)
            .hasMessage("L'ancien mot de passe est incorrect");

        verify(userRepository, never()).save(any(User.class));
    }
}
