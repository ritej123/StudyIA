package com.studyai.service;

import com.studyai.dto.request.CourseRequest;
import com.studyai.dto.response.CourseResponse;
import com.studyai.entity.Course;
import com.studyai.entity.CourseFile;
import com.studyai.exception.CourseNotFoundException;
import com.studyai.exception.InvalidDocumentException;
import com.studyai.repository.CourseFileRepository;
import com.studyai.repository.CourseRepository;
import com.studyai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseFileRepository courseFileRepository;
    private final UserRepository userRepository;
    private final DocumentStorageService documentStorage;
    private final StudyAiClient studyAiClient;

    @Transactional(readOnly = true)
    public List<CourseResponse> list(String email) {
        Long ownerId = userId(email);
        return courseRepository.findAllByUserId(ownerId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public CourseResponse create(String email, CourseRequest request) {
        Course course = Course.builder()
            .user(userRepository.findByEmail(email).orElseThrow())
            .title(request.title().trim())
            .subject(request.subject().trim())
            .description(request.description() == null ? "" : request.description().trim())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .files(new ArrayList<>())
            .build();
        return toResponse(courseRepository.save(course));
    }

    @Transactional
    public void delete(String email, Long courseId) {
        Course course = getCourse(email, courseId);
        deleteStoredFiles(course);
        courseRepository.delete(course);
    }

    @Transactional
    public void deleteAllForOwner(Long ownerId) {
        courseRepository.findAllByUserId(ownerId).forEach(course -> {
            deleteStoredFiles(course);
            courseRepository.delete(course);
        });
    }

    @Transactional
    public CourseResponse.CourseFileResponse upload(String email, Long courseId, MultipartFile file) {
        Course course = getCourse(email, courseId);
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new InvalidDocumentException("Le document doit avoir un nom de fichier.");
        }
        String filename = originalName.substring(
            Math.max(originalName.lastIndexOf('/'), originalName.lastIndexOf('\\')) + 1);
        if (filename.length() > 255) {
            throw new InvalidDocumentException("Le nom du fichier ne doit pas dépasser 255 caractères.");
        }

        String storageKey = documentStorage.store(file);
        try {
            String extractedText = studyAiClient.extract(originalName, documentStorage.load(storageKey));
            String extension = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
            CourseFile courseFile = CourseFile.builder()
                .course(course)
                .filename(filename)
                .contentType(extension.equals("pdf") ? "application/pdf" : "text/plain")
                .sizeBytes(file.getSize())
                .storageKey(storageKey)
                .extractedText(extractedText)
                .uploadedAt(Instant.now())
                .build();
            return toFileResponse(courseFileRepository.save(courseFile));
        } catch (RuntimeException ex) {
            documentStorage.delete(storageKey);
            throw ex;
        }
    }

    @Transactional
    public void deleteFile(String email, Long courseId, Long fileId) {
        Course course = getCourse(email, courseId);
        CourseFile file = courseFileRepository.findByIdAndCourseId(fileId, course.getId())
            .orElseThrow(() -> new InvalidDocumentException("Ce document n'existe pas dans le cours."));
        courseFileRepository.delete(file);
        if (file.getStorageKey() != null) {
            documentStorage.delete(file.getStorageKey());
        }
    }

    @Transactional(readOnly = true)
    public Course getCourse(String email, Long courseId) {
        Long ownerId = userId(email);
        return courseRepository.findByIdAndUserId(courseId, ownerId)
            .orElseThrow(() -> new CourseNotFoundException(courseId));
    }

    @Transactional(readOnly = true)
    public String getDocumentContext(String email, Long courseId, Long fileId) {
        Course course = getCourse(email, courseId);
        CourseFile file = courseFileRepository.findByIdAndCourseId(fileId, course.getId())
            .orElseThrow(() -> new CourseNotFoundException(fileId));
        String text = file.getExtractedText();
        if (text == null || text.isBlank()) {
            throw new InvalidDocumentException(
                "Ce document ne contient aucun texte exploitable pour générer une synthèse.");
        }
        return text.substring(0, Math.min(text.length(), 100_000));
    }

    @Transactional(readOnly = true)
    public String getCourseContext(String email, Long courseId) {
        Course course = getCourse(email, courseId);
        StringBuilder context = new StringBuilder();
        for (CourseFile file : courseFileRepository.findAllByCourseIdOrderByUploadedAtAsc(course.getId())) {
            String text = file.getExtractedText();
            if (text == null || text.isBlank() || context.length() >= 100_000) {
                continue;
            }
            if (!context.isEmpty()) {
                context.append("\n\n", 0, Math.min(2, 100_000 - context.length()));
            }
            int remaining = 100_000 - context.length();
            context.append(text, 0, Math.min(text.length(), remaining));
        }
        if (context.isEmpty()) {
            throw new InvalidDocumentException(
                "Ajoutez au moins un document avec du texte exploitable avant de lancer cet outil.");
        }
        return context.toString();
    }

    private Long userId(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié introuvable."))
            .getId();
    }

    private CourseResponse toResponse(Course course) {
        List<CourseResponse.CourseFileResponse> files = course.getFiles() == null
            ? List.of()
            : course.getFiles().stream().map(this::toFileResponse).toList();
        return new CourseResponse(course.getId(), course.getTitle(), course.getSubject(),
            course.getDescription(), course.getCreatedAt(), files);
    }

    private CourseResponse.CourseFileResponse toFileResponse(CourseFile file) {
        return new CourseResponse.CourseFileResponse(file.getId(), file.getFilename(),
            file.getContentType(), file.getSizeBytes(), file.getUploadedAt());
    }

    private void deleteStoredFiles(Course course) {
        if (course.getFiles() == null) {
            return;
        }
        course.getFiles().stream()
            .map(CourseFile::getStorageKey)
            .filter(key -> key != null)
            .forEach(documentStorage::delete);
    }
}
