package com.studyai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyai.dto.request.StudyAttemptRequest;
import com.studyai.dto.request.StudyGenerationRequest;
import com.studyai.dto.request.StudyQuestionRequest;
import com.studyai.dto.response.LearningResourceResponse;
import com.studyai.dto.response.StudyAttemptResponse;
import com.studyai.dto.response.StudyMessageResponse;
import com.studyai.entity.Course;
import com.studyai.entity.LearningResource;
import com.studyai.entity.StudyAttempt;
import com.studyai.entity.StudyMessage;
import com.studyai.entity.User;
import com.studyai.exception.InvalidDocumentException;
import com.studyai.exception.LearningResourceNotFoundException;
import com.studyai.exception.StudyAiException;
import com.studyai.repository.LearningResourceRepository;
import com.studyai.repository.StudyAttemptRepository;
import com.studyai.repository.StudyMessageRepository;
import com.studyai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyService {

    private final CourseService courseService;
    private final StudyAiClient aiClient;
    private final LearningResourceRepository resourceRepository;
    private final StudyMessageRepository messageRepository;
    private final StudyAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<LearningResourceResponse> listResources(String email) {
        Long ownerId = userId(email);
        return resourceRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
            .map(this::toResponse).toList();
    }

    @Transactional
    public LearningResourceResponse summarize(String email, Long courseId) {
        Course course = courseService.getCourse(email, courseId);
        String context = courseService.getCourseContext(email, courseId);
        String summary = aiClient.generate("summary", context, "", 0);
        return saveResource(email, course, "SUMMARY", "Synthèse - " + course.getTitle(), summary);
    }

    @Transactional
    public LearningResourceResponse generateQuiz(String email, Long courseId, StudyGenerationRequest request) {
        Course course = courseService.getCourse(email, courseId);
        String context = courseService.getCourseContext(email, courseId);
        String json = aiClient.generate("quiz", context, "", request.count());
        validateGeneratedContent(json, "questions", request.count(), true);
        return saveResource(email, course, "QUIZ", "Quiz - " + course.getTitle(), json);
    }

    @Transactional
    public LearningResourceResponse generateFlashcards(String email, Long courseId, StudyGenerationRequest request) {
        Course course = courseService.getCourse(email, courseId);
        String context = courseService.getCourseContext(email, courseId);
        String json = aiClient.generate("flashcards", context, "", request.count());
        validateGeneratedContent(json, "cards", request.count(), false);
        return saveResource(email, course, "FLASHCARDS", "Cartes mémoire - " + course.getTitle(), json);
    }

    @Transactional(readOnly = true)
    public List<StudyMessageResponse> getMessages(String email, Long courseId) {
        Course course = courseService.getCourse(email, courseId);
        return messageRepository.findAllByCourseIdAndOwnerIdOrderByIdAsc(course.getId(), userId(email))
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public StudyMessageResponse ask(String email, Long courseId, StudyQuestionRequest request) {
        Course course = courseService.getCourse(email, courseId);
        String context = courseService.getCourseContext(email, courseId);
        List<StudyMessage> history = messageRepository.findAllByCourseIdAndOwnerIdOrderByIdAsc(
            course.getId(), userId(email));
        String prompt = history.stream()
            .skip(Math.max(0, history.size() - 10))
            .map(message -> message.getRole() + ": " + message.getContent())
            .reduce("", (all, item) -> all + item + "\n");
        prompt += "student: " + request.question().trim();
        String answer = aiClient.generate("tutor", context, prompt, 0);

        User owner = userRepository.findByEmail(email).orElseThrow();
        Instant now = Instant.now();
        messageRepository.save(StudyMessage.builder()
            .owner(owner).course(course).role("student").content(request.question().trim()).createdAt(now).build());
        StudyMessage reply = messageRepository.save(StudyMessage.builder()
            .owner(owner).course(course).role("tutor").content(answer).createdAt(Instant.now()).build());
        return toResponse(reply);
    }

    @Transactional
    public StudyAttemptResponse submitAttempt(String email, Long resourceId, StudyAttemptRequest request) {
        Long ownerId = userId(email);
        LearningResource resource = resourceRepository.findByIdAndOwnerId(resourceId, ownerId)
            .orElseThrow(() -> new LearningResourceNotFoundException(resourceId));
        if (!resource.getKind().equals("QUIZ") && !resource.getKind().equals("FLASHCARDS")) {
            throw new InvalidDocumentException("Cette ressource ne peut pas être évaluée.");
        }

        JsonNode root = readJson(resource.getContent());
        JsonNode items = root.get(resource.getKind().equals("QUIZ") ? "questions" : "cards");
        List<Integer> answers = request.answers();
        if (items == null || !items.isArray() || items.isEmpty() || answers.size() != items.size()) {
            throw new InvalidDocumentException("Le nombre de réponses ne correspond pas à cette activité.");
        }

        int score = 0;
        for (int i = 0; i < answers.size(); i++) {
            Integer answer = answers.get(i);
            if (answer == null) {
                continue;
            }
            if (resource.getKind().equals("QUIZ")) {
                if (answer < 0 || answer >= items.get(i).path("options").size()) {
                    throw new InvalidDocumentException("Une réponse du quiz est invalide.");
                }
                if (answer == items.get(i).path("answer").asInt(-1)) {
                    score++;
                }
            } else {
                if (answer != 0 && answer != 1) {
                    throw new InvalidDocumentException("Une réponse de carte mémoire est invalide.");
                }
                if (answer == 1) {
                    score++;
                }
            }
        }

        StudyAttempt attempt = attemptRepository.save(StudyAttempt.builder()
            .owner(userRepository.findByEmail(email).orElseThrow())
            .resource(resource)
            .score(score)
            .total(items.size())
            .answers(writeJson(answers))
            .completedAt(Instant.now())
            .build());
        return new StudyAttemptResponse(attempt.getId(), resourceId, score, items.size());
    }

    private LearningResourceResponse saveResource(
            String email, Course course, String kind, String title, String content) {
        LearningResource resource = resourceRepository.save(LearningResource.builder()
            .owner(userRepository.findByEmail(email).orElseThrow())
            .course(course)
            .kind(kind)
            .title(title)
            .content(content)
            .createdAt(Instant.now())
            .build());
        return toResponse(resource);
    }

    private void validateGeneratedContent(String content, String key, int count, boolean quiz) {
        JsonNode root = readJson(content);
        JsonNode items = root != null && root.isObject() ? root.get(key) : null;
        if (items == null || !items.isArray() || items.size() != count) {
            throw new StudyAiException(502, "La réponse IA ne respecte pas le format demandé.");
        }
        for (JsonNode item : items) {
            if (quiz) {
                JsonNode options = item.get("options");
                int answer = item.path("answer").asInt(-1);
                if (item.path("question").asText().isBlank() || options == null || !options.isArray()
                    || options.size() != 4 || answer < 0 || answer >= options.size()
                    || item.path("explanation").asText().isBlank()
                    || !allOptionsAreText(options)) {
                    throw new StudyAiException(502, "Le quiz généré contient une question invalide.");
                }
            } else if (!item.path("front").isTextual() || item.path("front").asText().isBlank()
                || !item.path("back").isTextual() || item.path("back").asText().isBlank()) {
                throw new StudyAiException(502, "Les cartes mémoire générées sont incomplètes.");
            }
        }
    }

    private boolean allOptionsAreText(JsonNode options) {
        for (JsonNode option : options) {
            if (!option.isTextual() || option.asText().isBlank()) {
                return false;
            }
        }
        return true;
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new StudyAiException(502, "Le contenu généré n'est pas un JSON valide.");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Impossible d'enregistrer le résultat de l'activité.", ex);
        }
    }

    private Long userId(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié introuvable."))
            .getId();
    }

    private LearningResourceResponse toResponse(LearningResource resource) {
        return new LearningResourceResponse(resource.getId(), resource.getCourse().getId(), resource.getKind(),
            resource.getTitle(), resource.getContent(), resource.getCreatedAt());
    }

    private StudyMessageResponse toResponse(StudyMessage message) {
        return new StudyMessageResponse(message.getId(), message.getRole(), message.getContent(), message.getCreatedAt());
    }
}
