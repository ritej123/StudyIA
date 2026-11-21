import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, of, firstValueFrom } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private apiUrl = environment.apiUrl;

  private _currentUser = signal<AuthResponse | null>(null);
  private _accessToken: string | null = null;

  readonly currentUser = this._currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this._currentUser() !== null);
  readonly userRole = computed(() => this._currentUser()?.role ?? null);
  readonly userFullName = computed(() => {
    const u = this._currentUser();
    return u ? `${u.firstName} ${u.lastName}` : '';
  });

  getAccessToken(): string | null {
    return this._accessToken;
  }

  login(req: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, req, { withCredentials: true }).pipe(
      tap(response => this.setSession(response))
    );
  }

  register(req: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/users/register`, req, { withCredentials: true }).pipe(
      tap(response => this.setSession(response))
    );
  }

  logout(): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/logout`, {}, { withCredentials: true }).pipe(
      tap(() => this.clearSession()),
      catchError(() => {
        this.clearSession();
        return of(null);
      })
    );
  }

  refreshToken(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/refresh`, {}, { withCredentials: true }).pipe(
      tap(response => this.setSession(response))
    );
  }

  async initAuth(): Promise<void> {
    try {
      await firstValueFrom(this.refreshToken());
    } catch {
      // Pas de session active — normal
    }
  }

  updateCurrentUser(update: Partial<AuthResponse>): void {
    const current = this._currentUser();
    if (current) {
      this._currentUser.set({ ...current, ...update });
    }
  }

  private setSession(response: AuthResponse): void {
    this._accessToken = response.accessToken;
    this._currentUser.set(response);
  }

  private clearSession(): void {
    this._accessToken = null;
    this._currentUser.set(null);
  }
}
