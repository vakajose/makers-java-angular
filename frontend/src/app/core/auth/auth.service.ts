import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, UserProfile } from '../models/auth.model';

const TOKEN_STORAGE_KEY = 'makers_jwt_token';
const USER_STORAGE_KEY = 'makers_user_profile';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly authUrl = `${environment.apiUrl}/auth`;
  private readonly usersUrl = `${environment.apiUrl}/users`;

  private readonly _token = signal<string | null>(this.readStoredToken());
  private readonly _currentUser = signal<UserProfile | null>(this.readStoredUser());

  readonly token = this._token.asReadonly();
  readonly currentUser = this._currentUser.asReadonly();
  readonly isAuthenticated = computed(() => Boolean(this._token() && this._currentUser()));
  readonly isAdmin = computed(() => this._currentUser()?.role === 'ADMIN');

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.authUrl}/login`, payload)
      .pipe(tap((res) => this.persistSession(res.token, res.user)));
  }

  loadCurrentUser(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.usersUrl}/me`).pipe(
      tap((user) => {
        sessionStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
        this._currentUser.set(user);
      })
    );
  }

  logout(): void {
    sessionStorage.removeItem(TOKEN_STORAGE_KEY);
    sessionStorage.removeItem(USER_STORAGE_KEY);
    this._token.set(null);
    this._currentUser.set(null);
    void this.router.navigate(['/login']);
  }

  private persistSession(token: string, user: UserProfile): void {
    sessionStorage.setItem(TOKEN_STORAGE_KEY, token);
    sessionStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
    this._token.set(token);
    this._currentUser.set(user);
  }

  private readStoredToken(): string | null {
    return sessionStorage.getItem(TOKEN_STORAGE_KEY);
  }

  private readStoredUser(): UserProfile | null {
    const raw = sessionStorage.getItem(USER_STORAGE_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as UserProfile;
    } catch {
      sessionStorage.removeItem(USER_STORAGE_KEY);
      return null;
    }
  }
}
