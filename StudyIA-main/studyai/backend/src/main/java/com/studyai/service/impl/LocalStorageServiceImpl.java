package com.studyai.service.impl;

import com.studyai.exception.FileTooLargeException;
import com.studyai.exception.UnsupportedFileTypeException;
import com.studyai.service.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Implémentation locale du service de stockage de fichiers sur le système de fichiers.
 */
@Service
@Slf4j
public class LocalStorageServiceImpl implements StorageService {

    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10 Mo
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC  = {(byte) 0x89, 0x50, 0x4E, 0x47};
    // WebP : bytes 0-3 = RIFF, bytes 8-11 = WEBP
    private static final byte[] RIFF_MAGIC = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MAGIC = {0x57, 0x45, 0x42, 0x50};

    @Value("${app.upload.dir}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        rootLocation = Paths.get(uploadDir);
        try {
            Files.createDirectories(rootLocation);
            log.info("Répertoire de stockage initialisé : {}", rootLocation.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'initialiser le répertoire de stockage", e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        if (file.getSize() > MAX_SIZE) {
            throw new FileTooLargeException();
        }

        try {
            byte[] bytes = file.getBytes();
            validateMagicBytes(bytes);

            String extension = getExtension(bytes);
            String filename = UUID.randomUUID() + extension;
            Path destination = rootLocation.resolve(filename);
            Files.write(destination, bytes, StandardOpenOption.CREATE_NEW);
            log.info("Fichier sauvegardé : {}", filename);
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Échec de la sauvegarde du fichier", e);
        }
    }

    @Override
    public byte[] load(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            // Sécurité : vérifier que le chemin reste dans le répertoire de stockage
            if (!file.startsWith(rootLocation)) {
                throw new RuntimeException("Accès non autorisé");
            }
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new RuntimeException("Fichier introuvable : " + filename, e);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new RuntimeException("Accès non autorisé");
            }
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Impossible de supprimer le fichier : {}", filename);
        }
    }

    private void validateMagicBytes(byte[] bytes) {
        if (bytes.length < 12) throw new UnsupportedFileTypeException();
        if (startsWith(bytes, JPEG_MAGIC)) return;
        if (startsWith(bytes, PNG_MAGIC)) return;
        if (startsWith(bytes, RIFF_MAGIC) && containsAt(bytes, WEBP_MAGIC, 8)) return;
        throw new UnsupportedFileTypeException();
    }

    private String getExtension(byte[] bytes) {
        if (startsWith(bytes, JPEG_MAGIC)) return ".jpg";
        if (startsWith(bytes, PNG_MAGIC)) return ".png";
        return ".webp";
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private boolean containsAt(byte[] data, byte[] pattern, int offset) {
        if (data.length < offset + pattern.length) return false;
        for (int i = 0; i < pattern.length; i++) {
            if (data[offset + i] != pattern[i]) return false;
        }
        return true;
    }
}
