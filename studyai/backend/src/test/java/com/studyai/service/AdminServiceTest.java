package com.studyai.service;

import com.studyai.dto.request.AdminUpdateUserRequest;
import com.studyai.dto.request.UpdateUserStatusRequest;
import com.studyai.dto.response.AdminUserResponse;
import com.studyai.dto.response.PagedResponse;
import com.studyai.entity.User;
import com.studyai.entity.enums.Role;
import com.studyai.entity.enums.UserStatus;
import com.studyai.exception.UserNotFoundException;
import com.studyai.mapper.UserMapper;
import com.studyai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private TokenService tokenService;

    @Mock
    private CourseService courseService;

    @InjectMocks
    private AdminService adminService;

    private User sampleStudent;

    @BeforeEach
    void setUp() {
        sampleStudent = User.builder()
            .id(10L)
            .firstName("Sophie")
            .lastName("Durand")
            .email("sophie.durand@studyai.com")
            .passwordHash("hashedPass")
            .role(Role.ETUDIANT)
            .status(UserStatus.ACTIF)
            .build();
    }

    @Test
    @DisplayName("Récupération paginée de la liste des étudiants")
    void getStudents_ReturnsPage() {
        // Given
        Page<User> page = new PageImpl<>(List.of(sampleStudent));
        when(userRepository.findByRoleAndSearch(eq(Role.ETUDIANT), eq("Sophie"), any(Pageable.class)))
            .thenReturn(page);

        AdminUserResponse response = AdminUserResponse.builder()
            .id(10L)
            .firstName("Sophie")
            .lastName("Durand")
            .email("sophie.durand@studyai.com")
            .role(Role.ETUDIANT)
            .status(UserStatus.ACTIF)
            .build();

        when(userMapper.toAdminResponse(sampleStudent)).thenReturn(response);

        // When
        PagedResponse<AdminUserResponse> result = adminService.getStudents("Sophie", 0, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("sophie.durand@studyai.com");
        assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Modification réussie d'un utilisateur par l'administrateur")
    void updateUser_Success() {
        // Given
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setFirstName("Sophie Modif");
        request.setLastName("Durand Modif");
        request.setEmail("sophie.new@studyai.com");

        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleStudent));
        when(userRepository.save(sampleStudent)).thenReturn(sampleStudent);

        AdminUserResponse response = AdminUserResponse.builder()
            .id(10L)
            .firstName("Sophie Modif")
            .lastName("Durand Modif")
            .email("sophie.new@studyai.com")
            .build();
        when(userMapper.toAdminResponse(sampleStudent)).thenReturn(response);

        // When
        AdminUserResponse result = adminService.updateUser(10L, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(sampleStudent.getFirstName()).isEqualTo("Sophie Modif");
        assertThat(sampleStudent.getLastName()).isEqualTo("Durand Modif");
        assertThat(sampleStudent.getEmail()).isEqualTo("sophie.new@studyai.com");
        verify(userRepository).save(sampleStudent);
    }

    @Test
    @DisplayName("Modification échouée car utilisateur introuvable")
    void updateUser_NotFound() {
        // Given
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setFirstName("Inconnu");
        request.setLastName("User");
        request.setEmail("inconnu@studyai.com");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adminService.updateUser(999L, request))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining("999");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Suppression réussie d'un étudiant")
    void deleteUser_Success() {
        // Given
        when(userRepository.existsById(10L)).thenReturn(true);

        // When
        adminService.deleteUser(10L);

        // Then
        verify(userRepository).deleteById(10L);
        verify(courseService).deleteAllForOwner(10L);
    }

    @Test
    @DisplayName("Suppression échouée car étudiant introuvable")
    void deleteUser_NotFound() {
        // Given
        when(userRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> adminService.deleteUser(999L))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining("999");

        verify(userRepository, never()).deleteById(anyLong());
        verify(courseService, never()).deleteAllForOwner(anyLong());
    }

    @Test
    @DisplayName("Changement de statut en DESACTIVE révoque les tokens")
    void updateStatus_DeactivatesAndRevokesTokens() {
        // Given
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setStatus(UserStatus.DESACTIVE);

        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleStudent));
        when(userRepository.save(sampleStudent)).thenReturn(sampleStudent);

        AdminUserResponse response = AdminUserResponse.builder()
            .id(10L)
            .status(UserStatus.DESACTIVE)
            .build();
        when(userMapper.toAdminResponse(sampleStudent)).thenReturn(response);

        // When
        AdminUserResponse result = adminService.updateUserStatus(10L, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(sampleStudent.getStatus()).isEqualTo(UserStatus.DESACTIVE);
        verify(tokenService).revokeAllUserTokens(10L);
        verify(userRepository).save(sampleStudent);
    }
}
