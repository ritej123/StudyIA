package com.studyai.repository;

import com.studyai.entity.StudyAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyAttemptRepository extends JpaRepository<StudyAttempt, Long> {
    long count();
}
