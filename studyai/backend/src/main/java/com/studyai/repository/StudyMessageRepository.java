package com.studyai.repository;

import com.studyai.entity.StudyMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyMessageRepository extends JpaRepository<StudyMessage, Long> {
    List<StudyMessage> findAllByCourseIdAndOwnerIdOrderByIdAsc(Long courseId, Long ownerId);
}
