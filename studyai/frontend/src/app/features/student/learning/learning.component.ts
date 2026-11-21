import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import { forkJoin, switchMap } from 'rxjs';
import {
  Course,
  Flashcard,
  LearningResource,
  QuizQuestion,
  StudyMessage
} from '../../../core/models/learning.model';
import { LearningService } from '../../../core/services/learning.service';

@Component({
  selector: 'app-learning',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatIconModule, SkeletonLoaderComponent],
  templateUrl: './learning.component.html',
  styleUrls: ['./learning.component.scss']
})
export class LearningComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly learningService = inject(LearningService);

  readonly courses = signal<Course[]>([]);
  readonly resources = signal<LearningResource[]>([]);
  readonly messages = signal<StudyMessage[]>([]);
  readonly selectedCourseId = signal<number | null>(null);
  readonly activeResource = signal<LearningResource | null>(null);
  readonly selectedAnswers = signal<(number | null)[]>([]);
  readonly reviewedCards = signal<number[]>([]);
  readonly cardIndex = signal(0);
  readonly isCardFlipped = signal(false);
  readonly attemptResult = signal<{ score: number; total: number } | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly isLoading = signal(true);
  readonly isSavingCourse = signal(false);
  readonly isUploading = signal(false);
  readonly isGenerating = signal(false);
  readonly isSendingQuestion = signal(false);
  readonly isSubmitting = signal(false);

  readonly createCourseForm = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    subject: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', Validators.maxLength(1000)]
  });

  generatedCount = 5;
  tutorQuestion = '';

  readonly selectedCourse = computed(
    () => this.courses().find(course => course.id === this.selectedCourseId()) ?? null
  );

  ngOnInit(): void {
    this.loadWorkspace();
  }

  loadWorkspace(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    forkJoin({
      courses: this.learningService.getCourses(),
      resources: this.learningService.getResources()
    }).subscribe({
      next: ({ courses, resources }) => {
        this.courses.set(courses);
        this.resources.set(resources);
        this.isLoading.set(false);
        if (courses.length > 0) {
          this.selectCourse(courses[0].id);
        }
      },
      error: error => {
        this.isLoading.set(false);
        this.errorMessage.set(this.getErrorMessage(error));
      }
    });
  }

  selectCourse(courseId: number): void {
    this.selectedCourseId.set(courseId);
    this.activeResource.set(null);
    this.attemptResult.set(null);
    this.errorMessage.set(null);
    this.learningService.getMessages(courseId).subscribe({
      next: messages => this.messages.set(messages),
      error: error => this.errorMessage.set(this.getErrorMessage(error))
    });
  }

  createCourse(): void {
    if (this.createCourseForm.invalid || this.isSavingCourse()) {
      this.createCourseForm.markAllAsTouched();
      return;
    }
    this.isSavingCourse.set(true);
    this.errorMessage.set(null);
    this.learningService.createCourse(this.createCourseForm.getRawValue()).subscribe({
      next: course => {
        this.courses.update(courses => [course, ...courses]);
        this.createCourseForm.reset();
        this.isSavingCourse.set(false);
        this.selectCourse(course.id);
      },
      error: error => {
        this.isSavingCourse.set(false);
        this.errorMessage.set(this.getErrorMessage(error));
      }
    });
  }

  deleteCourse(course: Course): void {
    if (!window.confirm(`Supprimer le cours « ${course.title} » et ses ressources ?`)) {
      return;
    }
    this.errorMessage.set(null);
    this.learningService.deleteCourse(course.id).subscribe({
      next: () => {
        this.courses.update(courses => courses.filter(item => item.id !== course.id));
        this.resources.update(resources => resources.filter(item => item.courseId !== course.id));
        this.messages.set([]);
        this.selectedCourseId.set(this.courses()[0]?.id ?? null);
        this.activeResource.set(null);
        const nextCourseId = this.selectedCourseId();
        if (nextCourseId !== null) {
          this.selectCourse(nextCourseId);
        }
      },
      error: error => this.errorMessage.set(this.getErrorMessage(error))
    });
  }

  uploadDocument(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    const courseId = this.selectedCourseId();
    if (!file || courseId === null) {
      return;
    }
    if (file.size > 20 * 1024 * 1024) {
      this.errorMessage.set('Le fichier dépasse la limite de 20 Mo.');
      input.value = '';
      return;
    }
    this.isUploading.set(true);
    this.errorMessage.set(null);
    this.learningService.uploadDocument(courseId, file).subscribe({
      next: uploaded => {
        this.courses.update(courses => courses.map(course =>
          course.id === courseId ? { ...course, files: [...course.files, uploaded] } : course
        ));
        this.isUploading.set(false);
        input.value = '';
      },
      error: error => {
        this.isUploading.set(false);
        this.errorMessage.set(this.getErrorMessage(error));
        input.value = '';
      }
    });
  }

  deleteDocument(fileId: number): void {
    const courseId = this.selectedCourseId();
    if (courseId === null) {
      return;
    }
    this.learningService.deleteDocument(courseId, fileId).subscribe({
      next: () => this.courses.update(courses => courses.map(course =>
        course.id === courseId
          ? { ...course, files: course.files.filter(file => file.id !== fileId) }
          : course
      )),
      error: error => this.errorMessage.set(this.getErrorMessage(error))
    });
  }

  generateSummary(): void {
    this.runGeneration(courseId => this.learningService.summarize(courseId));
  }

  generateQuiz(): void {
    this.runGeneration(courseId => this.learningService.generateQuiz(courseId, this.generatedCount));
  }

  generateFlashcards(): void {
    this.runGeneration(courseId => this.learningService.generateFlashcards(courseId, this.generatedCount));
  }

  sendQuestion(): void {
    const courseId = this.selectedCourseId();
    const question = this.tutorQuestion.trim();
    if (courseId === null || !question || this.isSendingQuestion()) {
      return;
    }
    this.isSendingQuestion.set(true);
    this.errorMessage.set(null);
    this.learningService.askTutor(courseId, question).pipe(
      switchMap(() => this.learningService.getMessages(courseId))
    ).subscribe({
      next: messages => {
        this.messages.set(messages);
        this.tutorQuestion = '';
        this.isSendingQuestion.set(false);
      },
      error: error => {
        this.isSendingQuestion.set(false);
        this.errorMessage.set(this.getErrorMessage(error));
      }
    });
  }

  openResource(resource: LearningResource): void {
    this.activeResource.set(resource);
    this.attemptResult.set(null);
    this.reviewedCards.set([]);
    this.cardIndex.set(0);
    this.isCardFlipped.set(false);
    this.selectedAnswers.set(resource.kind === 'QUIZ' ? this.quizQuestions(resource).map(() => null) : []);
  }

  chooseAnswer(questionIndex: number, answerIndex: number): void {
    this.attemptResult.set(null);
    this.selectedAnswers.update(answers => answers.map((answer, index) =>
      index === questionIndex ? answerIndex : answer
    ));
  }

  submitQuiz(): void {
    const resource = this.activeResource();
    if (!resource || this.isSubmitting()) {
      return;
    }
    this.submitActivity(resource, this.selectedAnswers());
  }

  hasUnansweredQuiz(): boolean {
    return this.selectedAnswers().some(answer => answer === null);
  }

  flipFlashcard(): void {
    this.attemptResult.set(null);
    const index = this.cardIndex();
    this.isCardFlipped.update(flipped => !flipped);
    this.reviewedCards.update(cards => cards.includes(index) ? cards : [...cards, index]);
  }

  moveFlashcard(direction: number): void {
    const next = this.cardIndex() + direction;
    if (next >= 0 && next < this.flashcards().length) {
      this.attemptResult.set(null);
      this.cardIndex.set(next);
      this.isCardFlipped.set(false);
    }
  }

  submitFlashcards(): void {
    const resource = this.activeResource();
    if (!resource || this.isSubmitting()) {
      return;
    }
    const reviewed = new Set(this.reviewedCards());
    this.submitActivity(resource, this.flashcards().map((_, index) => reviewed.has(index) ? 1 : 0));
  }

  quizQuestions(resource = this.activeResource()): QuizQuestion[] {
    return this.readArray<QuizQuestion>(resource, 'questions', this.isQuizQuestion);
  }

  flashcards(resource = this.activeResource()): Flashcard[] {
    return this.readArray<Flashcard>(resource, 'cards', this.isFlashcard);
  }

  formatSize(size: number): string {
    return size < 1024 * 1024 ? `${Math.max(1, Math.round(size / 1024))} Ko` : `${(size / (1024 * 1024)).toFixed(1)} Mo`;
  }

  optionLetter(index: number): string {
    return String.fromCharCode(65 + index);
  }

  private runGeneration(generate: (courseId: number) => ReturnType<LearningService['summarize']>): void {
    const courseId = this.selectedCourseId();
    if (courseId === null || this.isGenerating()) {
      return;
    }
    this.isGenerating.set(true);
    this.errorMessage.set(null);
    generate(courseId).subscribe({
      next: resource => {
        this.resources.update(resources => [resource, ...resources]);
        this.openResource(resource);
        this.isGenerating.set(false);
      },
      error: error => {
        this.isGenerating.set(false);
        this.errorMessage.set(this.getErrorMessage(error));
      }
    });
  }

  private submitActivity(resource: LearningResource, answers: (number | null)[]): void {
    this.isSubmitting.set(true);
    this.errorMessage.set(null);
    this.learningService.submitAttempt(resource.id, answers).subscribe({
      next: result => {
        this.attemptResult.set({ score: result.score, total: result.total });
        this.isSubmitting.set(false);
      },
      error: error => {
        this.isSubmitting.set(false);
        this.errorMessage.set(this.getErrorMessage(error));
      }
    });
  }

  private readArray<T>(
    resource: LearningResource | null,
    key: string,
    isItem: (value: unknown) => value is T
  ): T[] {
    if (!resource) {
      return [];
    }
    try {
      const parsed: unknown = JSON.parse(resource.content);
      if (typeof parsed !== 'object' || parsed === null || !(key in parsed)) {
        return [];
      }
      const value: unknown = (parsed as Record<string, unknown>)[key];
      return Array.isArray(value) ? value.filter(isItem) : [];
    } catch (error) {
      if (!(error instanceof SyntaxError)) {
        throw error;
      }
      this.errorMessage.set('Le contenu enregistré de cette activité est illisible.');
      return [];
    }
  }

  private isQuizQuestion(value: unknown): value is QuizQuestion {
    if (typeof value !== 'object' || value === null) {
      return false;
    }
    const question = value as Partial<QuizQuestion>;
    return typeof question.question === 'string'
      && Array.isArray(question.options)
      && question.options.every(option => typeof option === 'string')
      && typeof question.answer === 'number'
      && typeof question.explanation === 'string';
  }

  private isFlashcard(value: unknown): value is Flashcard {
    if (typeof value !== 'object' || value === null) {
      return false;
    }
    const card = value as Partial<Flashcard>;
    return typeof card.front === 'string' && typeof card.back === 'string';
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      return error.error.message;
    }
    if (error instanceof HttpErrorResponse && typeof error.error?.detail === 'string') {
      return error.error.detail;
    }
    return 'Une erreur est survenue pendant le chargement. Réessayez.';
  }
}
