package com.example.app.service;

import com.example.app.entity.Document;
import com.example.app.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Set<String> ALLOWED = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "txt", "xls", "xlsx");

    private final DocumentRepository repo;
    private final Path root;

    public DocumentService(DocumentRepository repo,
                           @Value("${file.upload-dir}") String uploadDir) throws IOException {
        this.repo = repo;
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public Document upload(MultipartFile file, Long ownerId) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Fichier vide");
        }

        String original = Paths.get(file.getOriginalFilename()).getFileName().toString();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOWED.contains(ext)) {
            throw new IllegalArgumentException("Type de fichier non autorisé : " + ext);
        }

        String storedName = UUID.randomUUID() + "." + ext;
        Files.copy(file.getInputStream(), root.resolve(storedName),
                StandardCopyOption.REPLACE_EXISTING);

        Document doc = new Document();
        doc.setOriginalName(original);
        doc.setStoredName(storedName);
        doc.setContentType(file.getContentType());
        doc.setSize(file.getSize());
        doc.setOwnerId(ownerId);
        return repo.save(doc);
    }

    public List<Document> list(Long ownerId) {
        return repo.findByOwnerIdOrderByUploadedAtDesc(ownerId);
    }

    public Document get(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Document introuvable"));
    }

    public Resource load(Document doc) throws IOException {
        Path path = root.resolve(doc.getStoredName()).normalize();
        Resource res = new UrlResource(path.toUri());
        if (!res.exists()) throw new RuntimeException("Fichier introuvable sur le disque");
        return res;
    }

    public void delete(Long id) throws IOException {
        Document doc = get(id);
        Files.deleteIfExists(root.resolve(doc.getStoredName()));
        repo.delete(doc);
    }
}