export interface CourseFile {
  id: number;
  filename: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
}

export interface Course {
  id: number;
  title: string;
  subject: string;
  description: string;
  createdAt: string;
  files: CourseFile[];
}

export interface CourseRequest {
  title: string;
  subject: string;
  description: string;
}

export type LearningResourceKind = 'SUMMARY' | 'QUIZ' | 'FLASHCARDS';

export interface LearningResource {
  id: number;
  courseId: number;
  kind: LearningResourceKind;
  title: string;
  content: string;
  createdAt: string;
}

export interface StudyMessage {
  id: number;
  role: 'student' | 'tutor';
  content: string;
  createdAt: string;
}

export interface StudyAttempt {
  id: number;
  resourceId: number;
  score: number;
  total: number;
}

export interface QuizQuestion {
  question: string;
  options: string[];
  answer: number;
  explanation: string;
}

export interface Flashcard {
  front: string;
  back: string;
}
