import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Course,
  CourseFile,
  CourseRequest,
  LearningResource,
  StudyAttempt,
  StudyMessage
} from '../models/learning.model';

@Injectable({ providedIn: 'root' })
export class LearningService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getCourses(): Observable<Course[]> {
    return this.http.get<Course[]>(`${this.apiUrl}/courses`);
  }

  createCourse(request: CourseRequest): Observable<Course> {
    return this.http.post<Course>(`${this.apiUrl}/courses`, request);
  }

  deleteCourse(courseId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/courses/${courseId}`);
  }

  uploadDocument(courseId: number, file: File): Observable<CourseFile> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<CourseFile>(`${this.apiUrl}/courses/${courseId}/files`, formData);
  }

  deleteDocument(courseId: number, fileId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/courses/${courseId}/files/${fileId}`);
  }

  getResources(): Observable<LearningResource[]> {
    return this.http.get<LearningResource[]>(`${this.apiUrl}/study/resources`);
  }

  summarize(courseId: number): Observable<LearningResource> {
    return this.http.post<LearningResource>(`${this.apiUrl}/study/courses/${courseId}/summary`, {});
  }

  summarizeFile(courseId: number, fileId: number): Observable<LearningResource> {
    return this.http.post<LearningResource>(
      `${this.apiUrl}/study/courses/${courseId}/files/${fileId}/summary`, {});
  }

  exportSummaryPdf(resourceId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/study/resources/${resourceId}/export/pdf`, {
      responseType: 'blob'
    });
  }

  generateQuiz(courseId: number, count: number): Observable<LearningResource> {
    return this.http.post<LearningResource>(`${this.apiUrl}/study/courses/${courseId}/quiz`, { count });
  }

  generateFlashcards(courseId: number, count: number): Observable<LearningResource> {
    return this.http.post<LearningResource>(`${this.apiUrl}/study/courses/${courseId}/flashcards`, { count });
  }

  getMessages(courseId: number): Observable<StudyMessage[]> {
    return this.http.get<StudyMessage[]>(`${this.apiUrl}/study/courses/${courseId}/messages`);
  }

  askTutor(courseId: number, question: string): Observable<StudyMessage> {
    return this.http.post<StudyMessage>(`${this.apiUrl}/study/courses/${courseId}/messages`, { question });
  }

  submitAttempt(resourceId: number, answers: (number | null)[]): Observable<StudyAttempt> {
    return this.http.post<StudyAttempt>(`${this.apiUrl}/study/resources/${resourceId}/attempts`, { answers });
  }
}
