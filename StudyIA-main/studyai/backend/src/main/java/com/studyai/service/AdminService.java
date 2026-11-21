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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * Service gérant les opérations d'administration sur les utilisateurs et étudiants.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final TokenService tokenService;
    private final CourseService courseService;

    /**
     * Liste paginée des étudiants avec recherche textuelle.
     */
    @Transactional(readOnly = true)
    public PagedResponse<AdminUserResponse> getStudents(String search, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String searchParam = (search == null || search.isBlank()) ? null : search.trim();
        Page<User> userPage;
        if (searchParam == null) {
            userPage = userRepository.findByRole(Role.ETUDIANT, pageable);
        } else {
            userPage = userRepository.findByRoleAndSearch(Role.ETUDIANT, searchParam, pageable);
        }

        return PagedResponse.<AdminUserResponse>builder()
            .content(userPage.getContent().stream().map(userMapper::toAdminResponse).collect(Collectors.toList()))
            .pageNumber(userPage.getNumber())
            .pageSize(userPage.getSize())
            .totalElements(userPage.getTotalElements())
            .totalPages(userPage.getTotalPages())
            .last(userPage.isLast())
            .build();
    }

    /**
     * Modifie les informations d'un étudiant.
     */
    @Transactional
    public AdminUserResponse updateUser(Long id, AdminUpdateUserRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user = userRepository.save(user);
        log.info("Admin a modifié le compte : {}", id);
        return userMapper.toAdminResponse(user);
    }

    /**
     * Modifie le statut d'un compte utilisateur.
     */
    @Transactional
    public AdminUserResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));

        user.setStatus(request.getStatus());
        user = userRepository.save(user);

        // Révoquer tous les tokens si le compte est désactivé
        if (request.getStatus() == UserStatus.DESACTIVE) {
            tokenService.revokeAllUserTokens(id);
            log.info("Compte désactivé et tokens révoqués : {}", id);
        }

        return userMapper.toAdminResponse(user);
    }

    /**
     * Supprime un compte étudiant (suppression définitive).
     */
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        courseService.deleteAllForOwner(id);
        userRepository.deleteById(id);
        log.info("Admin a supprimé le compte : {}", id);
    }
}
