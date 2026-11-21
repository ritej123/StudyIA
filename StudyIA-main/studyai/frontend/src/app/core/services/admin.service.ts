import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AdminUser } from '../models/user.model';
import { AdminStats, MessageResponse, PagedResponse } from '../models/api-response.model';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  getStats(): Observable<AdminStats> {
    return this.http.get<AdminStats>(`${this.apiUrl}/admin/stats`);
  }

  getStudents(search?: string, page = 0, size = 10): Observable<PagedResponse<AdminUser>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (search && search.trim().length > 0) {
      params = params.set('search', search.trim());
    }
    return this.http.get<PagedResponse<AdminUser>>(`${this.apiUrl}/admin/users`, { params });
  }

  updateUser(id: number, data: { firstName: string; lastName: string; email: string }): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${this.apiUrl}/admin/users/${id}`, data);
  }

  updateStatus(id: number, status: string): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${this.apiUrl}/admin/users/${id}/status`, { status });
  }

  deleteUser(id: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${this.apiUrl}/admin/users/${id}`);
  }
}
