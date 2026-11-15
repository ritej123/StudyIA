package com.studyai.repository;

import com.studyai.entity.CourseFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseFileRepository extends JpaRepository<CourseFile, Long> {
    List<CourseFile> findAllByCourseId(Long courseId);
    List<CourseFile> findAllByCourseIdOrderByUploadedAtAsc(Long courseId);
    Optional<CourseFile> findByIdAndCourseId(Long id, Long courseId);
}
