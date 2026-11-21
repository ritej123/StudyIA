package com.studyai.repository;

import com.studyai.entity.LearningResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {
    List<LearningResource> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    Optional<LearningResource> findByIdAndOwnerId(Long id, Long ownerId);
    long countByKind(String kind);
}
