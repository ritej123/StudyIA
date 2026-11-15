package com.studyai.service;

import com.studyai.dto.response.AdminStatsResponse;
import com.studyai.entity.enums.Role;
import com.studyai.entity.enums.UserStatus;
import com.studyai.repository.CourseFileRepository;
import com.studyai.repository.CourseRepository;
import com.studyai.repository.LearningResourceRepository;
import com.studyai.repository.StudyAttemptRepository;
import com.studyai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseFileRepository courseFileRepository;
    private final LearningResourceRepository resourceRepository;
    private final StudyAttemptRepository attemptRepository;

    @Transactional(readOnly = true)
    public AdminStatsResponse getStats() {
        return new AdminStatsResponse(
            userRepository.countByRole(Role.ETUDIANT),
            userRepository.countByRoleAndStatus(Role.ETUDIANT, UserStatus.ACTIF),
            userRepository.countByRoleAndStatus(Role.ETUDIANT, UserStatus.EN_ATTENTE),
            userRepository.countByRoleAndStatus(Role.ETUDIANT, UserStatus.DESACTIVE),
            courseRepository.count(),
            courseFileRepository.count(),
            resourceRepository.count(),
            resourceRepository.countByKind("SUMMARY"),
            resourceRepository.countByKind("QUIZ"),
            resourceRepository.countByKind("FLASHCARDS"),
            attemptRepository.count()
        );
    }
}
