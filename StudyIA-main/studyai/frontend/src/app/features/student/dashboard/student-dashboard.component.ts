import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { LearningService } from '../../../core/services/learning.service';
import { Course, LearningResource } from '../../../core/models/learning.model';
import { CircularProgressComponent } from '../../../shared/components/circular-progress/circular-progress.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';

interface ProgressStat {
  label: string;
  value: number;
  icon: string;
  color: string;
  link?: string;
}

@Component({
  selector: 'app-student-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    CircularProgressComponent,
    EmptyStateComponent,
    SkeletonLoaderComponent
  ],
  templateUrl: './student-dashboard.component.html',
  styleUrls: ['./student-dashboard.component.scss']
})
export class StudentDashboardComponent implements OnInit {
  readonly authService = inject(AuthService);
  readonly router = inject(Router);
  private readonly learningService = inject(LearningService);

  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly courses = signal<Course[]>([]);
  readonly resources = signal<LearningResource[]>([]);

  readonly documentCount = computed(() =>
    this.courses().reduce((total, course) => total + course.files.length, 0)
  );

  readonly summaryCount = computed(() =>
    this.resources().filter(resource => resource.kind === 'SUMMARY').length
  );

  readonly quizCount = computed(() =>
    this.resources().filter(resource => resource.kind === 'QUIZ').length
  );

  readonly flashcardCount = computed(() =>
    this.resources().filter(resource => resource.kind === 'FLASHCARDS').length
  );

  readonly totalActivities = computed(() =>
    this.summaryCount() + this.quizCount() + this.flashcardCount()
  );

  readonly progressPct = computed(() => {
    const total = this.totalActivities();
    if (total === 0) return 0;
    return Math.min(100, Math.round((total / Math.max(10, total)) * 100));
  });

  readonly progressStats: ProgressStat[] = [];

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
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
      },
      error: () => {
        this.errorMessage.set('Impossible de charger vos données de révision.');
        this.isLoading.set(false);
      }
    });
  }

  readonly stats = computed<ProgressStat[]>(() => [
    {
      label: 'Cours suivis',
      value: this.courses().length,
      icon: 'menu_book',
      color: 'var(--primary-blue)',
      link: '/student/learning'
    },
    {
      label: 'Documents',
      value: this.documentCount(),
      icon: 'description',
      color: 'var(--primary-violet)',
      link: '/student/learning'
    },
    {
      label: 'Synthèses IA',
      value: this.summaryCount(),
      icon: 'summarize',
      color: 'var(--success)',
      link: '/student/learning'
    },
    {
      label: 'Quiz & Flashcards',
      value: this.quizCount() + this.flashcardCount(),
      icon: 'quiz',
      color: 'var(--warning)',
      link: '/student/learning'
    }
  ]);

  readonly recentResources = computed(() => this.resources().slice(0, 4));
}
