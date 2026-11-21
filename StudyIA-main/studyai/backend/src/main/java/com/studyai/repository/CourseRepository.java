package com.studyai.repository;

import com.studyai.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findAllByUserId(Long userId);
    java.util.Optional<Course> findByIdAndUserId(Long id, Long userId);
}
