package com.example.app.repository;

import com.example.app.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    long countByOwnerId(Long ownerId);
    List<Document> findByOwnerIdOrderByUploadedAtDesc(Long ownerId);
}