import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User } from '../models/user.model';
import { UpdateProfileRequest, ChangePasswordRequest } from '../models/auth.model';
import { MessageResponse } from '../models/api-response.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  getProfile(): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/users/profile`);
  }

  updateProfile(req: UpdateProfileRequest): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/users/profile`, req);
  }

  uploadPhoto(file: File): Observable<User> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<User>(`${this.apiUrl}/users/profile/photo`, formData);
  }

  getPhotoUrl(filename: string): string {
    return `${this.apiUrl}/users/profile/photo/${filename}`;
  }

  changePassword(req: ChangePasswordRequest): Observable<MessageResponse> {
    return this.http.put<MessageResponse>(`${this.apiUrl}/users/password`, req);
  }
}
