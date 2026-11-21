package com.studyai.repository;

import com.studyai.entity.User;
import com.studyai.entity.enums.Role;
import com.studyai.entity.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository JPA pour la gestion des utilisateurs.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    long countByRoleAndStatus(Role role, UserStatus status);

    @Query("SELECT u FROM User u WHERE u.role = :role ORDER BY u.createdAt DESC")
    Page<User> findByRole(@Param("role") Role role, Pageable pageable);

    @Query(value = """
        SELECT u.* FROM users u
        WHERE u.role = :role
          AND (:search IS NULL
               OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :search::VARCHAR, '%'))
               OR LOWER(u.last_name)  LIKE LOWER(CONCAT('%', :search::VARCHAR, '%'))
               OR LOWER(u.email)      LIKE LOWER(CONCAT('%', :search::VARCHAR, '%')))
        ORDER BY u.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM users u
        WHERE u.role = :role
          AND (:search IS NULL
               OR LOWER(u.first_name) LIKE LOWER(CONCAT('%', :search::VARCHAR, '%'))
               OR LOWER(u.last_name)  LIKE LOWER(CONCAT('%', :search::VARCHAR, '%'))
               OR LOWER(u.email)      LIKE LOWER(CONCAT('%', :search::VARCHAR, '%')))
        """,
        nativeQuery = true)
    Page<User> findByRoleAndSearch(@Param("role") Role role, @Param("search") String search, Pageable pageable);
}
