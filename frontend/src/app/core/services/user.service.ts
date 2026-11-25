import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserProfile } from '../models/auth.model';
import { extractErrorMessage } from '../models/problem-detail.model';
import { CreateUserRequest, UpdateUserRequest } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/users`;

  private readonly _users = signal<UserProfile[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly users = this._users.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  clearError(): void {
    this._error.set(null);
  }

  loadUsers(preserveError = false): Observable<UserProfile[]> {
    this._loading.set(true);
    if (!preserveError) {
      this._error.set(null);
    }
    return this.http.get<UserProfile[]>(this.baseUrl).pipe(
      tap((items) => this._users.set(items)),
      catchError((err) => {
        this._error.set(extractErrorMessage(err, 'No se pudo cargar la lista de usuarios.'));
        return throwError(() => err);
      }),
      finalize(() => this._loading.set(false))
    );
  }

  createUser(payload: CreateUserRequest): Observable<UserProfile> {
    this._error.set(null);
    return this.http.post<UserProfile>(this.baseUrl, payload).pipe(
      tap((created) => this._users.update((list) => [...list, created])),
      catchError((err) => {
        this._error.set(extractErrorMessage(err, 'No se pudo crear el usuario.'));
        return throwError(() => err);
      })
    );
  }

  updateUser(userId: number, payload: UpdateUserRequest): Observable<UserProfile> {
    this._error.set(null);
    return this.http.put<UserProfile>(`${this.baseUrl}/${userId}`, payload).pipe(
      tap((updated) =>
        this._users.update((list) =>
          list.map((item) => (item.id === updated.id ? updated : item))
        )
      ),
      catchError((err) => {
        this._error.set(extractErrorMessage(err, 'No se pudo actualizar el usuario.'));
        return throwError(() => err);
      })
    );
  }

  deleteUser(userId: number): Observable<void> {
    this._error.set(null);
    return this.http.delete<void>(`${this.baseUrl}/${userId}`).pipe(
      tap(() =>
        this._users.update((list) => list.filter((item) => item.id !== userId))
      ),
      catchError((err) => {
        this._error.set(extractErrorMessage(err, 'No se pudo eliminar el usuario.'));
        return throwError(() => err);
      })
    );
  }
}
