package com.studyai.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Interface du service de stockage de fichiers.
 */
public interface StorageService {
    /** Sauvegarde le fichier et retourne son nom UUID */
    String store(MultipartFile file);

    /** Retourne les bytes du fichier par son nom UUID */
    byte[] load(String filename);

    /** Supprime le fichier */
    void delete(String filename);
}
