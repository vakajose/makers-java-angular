import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateLoanRequest, Loan } from '../models/loan.model';
import { extractErrorMessage } from '../models/problem-detail.model';

@Injectable({ providedIn: 'root' })
export class LoanService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/loans`;

  private readonly _loans = signal<Loan[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly loans = this._loans.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  clearError(): void {
    this._error.set(null);
  }

  loadMyLoans(): Observable<Loan[]> {
    this._loading.set(true);
    this._error.set(null);
    return this.http.get<Loan[]>(`${this.baseUrl}/my`).pipe(
      tap((items) => this._loans.set(items)),
      catchError((err) => {
        this._error.set(extractErrorMessage(err, 'No se pudieron cargar tus préstamos.'));
        return throwError(() => err);
      }),
      finalize(() => this._loading.set(false))
    );
  }

  loadAllLoans(preserveError = false): Observable<Loan[]> {
    this._loading.set(true);
    if (!preserveError) {
      this._error.set(null);
    }
    return this.http.get<Loan[]>(this.baseUrl).pipe(
      tap((items) => this._loans.set(items)),
      catchError((err) => {
        this._error.set(extractErrorMessage(err, 'No se pudieron cargar las solicitudes de préstamo.'));
        return throwError(() => err);
      }),
      finalize(() => this._loading.set(false))
    );
  }

  requestLoan(payload: CreateLoanRequest): Observable<Loan> {
    this._loading.set(true);
    this._error.set(null);
    return this.http.post<Loan>(this.baseUrl, payload).pipe(
      tap((created) => this._loans.update((list) => [created, ...list])),
      catchError((err) => {
        this._error.set(extractErrorMessage(err, 'No se pudo registrar la solicitud de préstamo.'));
        return throwError(() => err);
      }),
      finalize(() => this._loading.set(false))
    );
  }

  approveLoan(loanId: number): Observable<Loan> {
    this._error.set(null);
    return this.http.patch<Loan>(`${this.baseUrl}/${loanId}/approve`, {}).pipe(
      tap((updated) => this.replaceLoan(updated)),
      catchError((err) => {
        this._error.set(extractErrorMessage(err, 'No se pudo aprobar el préstamo.'));
        return throwError(() => err);
      })
    );
  }

  rejectLoan(loanId: number): Observable<Loan> {
    this._error.set(null);
    return this.http.patch<Loan>(`${this.baseUrl}/${loanId}/reject`, {}).pipe(
      tap((updated) => this.replaceLoan(updated)),
      catchError((err) => {
        this._error.set(extractErrorMessage(err, 'No se pudo rechazar el préstamo.'));
        return throwError(() => err);
      })
    );
  }

  cancelLoan(loanId: number): Observable<Loan> {
    this._error.set(null);
    return this.http.delete<Loan>(`${this.baseUrl}/${loanId}`).pipe(
      tap((updated) => this.replaceLoan(updated)),
      catchError((err) => {
        this._error.set(extractErrorMessage(err, 'No se pudo cancelar el préstamo.'));
        return throwError(() => err);
      })
    );
  }

  private replaceLoan(updated: Loan): void {
    this._loans.update((list) =>
      list.map((item) => (item.id === updated.id ? updated : item))
    );
  }
}
