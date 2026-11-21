package com.studyai.service;

import com.studyai.exception.InvalidDocumentException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class DocumentStorageService {

    private static final long MAX_SIZE = 10 * 1024 * 1024;
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    @Value("${app.upload.dir}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    void init() {
        rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible d'initialiser le stockage des documents.", ex);
        }
    }

    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidDocumentException("Le document est vide.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new InvalidDocumentException("Le document dépasse la limite de 10 Mo.");
        }

        String originalName = file.getOriginalFilename();
        String extension = originalName == null || !originalName.contains(".")
            ? "" : originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
        if (!extension.equals("pdf") && !extension.equals("txt")) {
            throw new InvalidDocumentException("Seuls les fichiers PDF et TXT sont acceptés.");
        }

        try {
            byte[] bytes = file.getBytes();
            if (extension.equals("pdf") && !startsWith(bytes, PDF_MAGIC)) {
                throw new InvalidDocumentException("Le fichier transmis n'est pas un PDF valide.");
            }
            String storageKey = UUID.randomUUID() + "." + extension;
            Files.write(rootLocation.resolve(storageKey), bytes);
            return storageKey;
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible d'enregistrer le document.", ex);
        }
    }

    public byte[] load(String storageKey) {
        return resolve(storageKey)
            .map(path -> {
                try {
                    return Files.readAllBytes(path);
                } catch (IOException ex) {
                    throw new IllegalStateException("Impossible de lire le document enregistré.", ex);
                }
            })
            .orElseThrow(() -> new IllegalStateException("Le document enregistré est introuvable."));
    }

    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey)
                .orElseThrow(() -> new IllegalArgumentException("Clé de stockage invalide.")));
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible de supprimer le document enregistré.", ex);
        }
    }

    private java.util.Optional<Path> resolve(String storageKey) {
        Path path = rootLocation.resolve(storageKey).normalize();
        if (!path.startsWith(rootLocation) || storageKey.contains("/") || storageKey.contains("\\")) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(path);
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
