package com.studyai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyai.dto.request.StudyAttemptRequest;
import com.studyai.dto.response.StudyAttemptResponse;
import com.studyai.entity.LearningResource;
import com.studyai.entity.StudyAttempt;
import com.studyai.entity.User;
import com.studyai.exception.InvalidDocumentException;
import com.studyai.repository.LearningResourceRepository;
import com.studyai.repository.StudyAttemptRepository;
import com.studyai.repository.StudyMessageRepository;
import com.studyai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyServiceTest {

    @Mock
    private CourseService courseService;
    @Mock
    private StudyAiClient aiClient;
    @Mock
    private LearningResourceRepository resourceRepository;
    @Mock
    private StudyMessageRepository messageRepository;
    @Mock
    private StudyAttemptRepository attemptRepository;
    @Mock
    private UserRepository userRepository;

    private StudyService studyService;
    private User owner;
    private LearningResource quiz;

    @BeforeEach
    void setUp() {
        studyService = new StudyService(courseService, aiClient, resourceRepository, messageRepository,
            attemptRepository, userRepository, new ObjectMapper());
        owner = User.builder().id(5L).email("student@studyai.com").build();
        quiz = LearningResource.builder()
            .id(12L)
            .owner(owner)
            .kind("QUIZ")
            .content("{\"questions\":["
                + "{\"question\":\"Q1\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"answer\":1},"
                + "{\"question\":\"Q2\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"answer\":2}]}")
            .build();
    }

    @Test
    void submitAttemptScoresAnswersAndPersistsResult() {
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(resourceRepository.findByIdAndOwnerId(quiz.getId(), owner.getId())).thenReturn(Optional.of(quiz));
        when(attemptRepository.save(any(StudyAttempt.class))).thenAnswer(invocation -> {
            StudyAttempt attempt = invocation.getArgument(0);
            attempt.setId(99L);
            return attempt;
        });

        StudyAttemptResponse result = studyService.submitAttempt(
            owner.getEmail(), quiz.getId(), new StudyAttemptRequest(List.of(1, 0)));

        assertThat(result).isEqualTo(new StudyAttemptResponse(99L, 12L, 1, 2));
        verify(attemptRepository).save(argThat(attempt ->
            attempt.getOwner().getId().equals(owner.getId())
                && attempt.getScore() == 1
                && attempt.getTotal() == 2
                && attempt.getAnswers().equals("[1,0]")));
    }

    @Test
    void submitAttemptRejectsInvalidAnswerIndexes() {
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(resourceRepository.findByIdAndOwnerId(quiz.getId(), owner.getId())).thenReturn(Optional.of(quiz));

        assertThatThrownBy(() -> studyService.submitAttempt(
            owner.getEmail(), quiz.getId(), new StudyAttemptRequest(List.of(4, 0))))
            .isInstanceOf(InvalidDocumentException.class)
            .hasMessageContaining("invalide");

        verify(attemptRepository, never()).save(any());
    }
}
