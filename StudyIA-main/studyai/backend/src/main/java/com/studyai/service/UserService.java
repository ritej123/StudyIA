package com.studyai.service;

import com.studyai.dto.request.ChangePasswordRequest;
import com.studyai.dto.request.UpdateProfileRequest;
import com.studyai.dto.response.UserProfileResponse;
import com.studyai.entity.User;
import com.studyai.exception.InvalidPasswordException;
import com.studyai.exception.UserNotFoundException;
import com.studyai.mapper.UserMapper;
import com.studyai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service gérant le profil de l'utilisateur, sa photo et la modification de mot de passe.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;

    /**
     * Récupère le profil de l'utilisateur connecté.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException(email));
        return userMapper.toProfileResponse(user);
    }

    /**
     * Modifie le profil (nom, prénom).
     */
    @Transactional
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException(email));

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user = userRepository.save(user);
        log.info("Profil mis à jour pour : {}", email);
        return userMapper.toProfileResponse(user);
    }

    /**
     * Upload de la photo de profil.
     */
    @Transactional
    public UserProfileResponse uploadPhoto(String email, MultipartFile file) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException(email));

        // Supprimer l'ancienne photo si elle existe
        if (user.getPhotoUrl() != null) {
            try {
                storageService.delete(user.getPhotoUrl());
            } catch (Exception ignored) {}
        }

        String filename = storageService.store(file);
        user.setPhotoUrl(filename);
        user = userRepository.save(user);
        log.info("Photo mise à jour pour : {}", email);
        return userMapper.toProfileResponse(user);
    }

    /**
     * Modifie le mot de passe (vérification de l'ancien mot de passe).
     */
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException(email));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException();
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Mot de passe changé pour : {}", email);
    }
}
