export interface ErrorResponse {
  status: number;
  code: string;
  message: string;
  errors?: Record<string, string>;
  timestamp: string;
}

export interface PagedResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface MessageResponse {
  message: string;
}

export interface AdminStats {
  totalStudents: number;
  activeStudents: number;
  pendingStudents: number;
  disabledStudents: number;
  courses: number;
  documents: number;
  learningResources: number;
  summaries: number;
  quizzes: number;
  flashcardSets: number;
  completedActivities: number;
}
