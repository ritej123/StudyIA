import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DocumentDto {
  id: number;
  originalName: string;
  contentType: string;
  size: number;
  ownerId: number;
  uploadedAt: string;
}

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private http = inject(HttpClient);
  private api = 'http://localhost:8080/api/documents';

  upload(file: File, ownerId: number): Observable<DocumentDto> {
    const form = new FormData();
    form.append('file', file);
    form.append('ownerId', String(ownerId));
    return this.http.post<DocumentDto>(`${this.api}/upload`, form);
  }

  list(ownerId: number): Observable<DocumentDto[]> {
    return this.http.get<DocumentDto[]>(`${this.api}/owner/${ownerId}`);
  }

  download(id: number): Observable<Blob> {
    return this.http.get(`${this.api}/${id}/download`, { responseType: 'blob' });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}