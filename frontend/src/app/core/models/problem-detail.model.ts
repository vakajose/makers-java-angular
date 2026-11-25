import { HttpErrorResponse } from '@angular/common/http';

export interface ProblemDetail {
  title?: string;
  status?: number;
  detail?: string;
  errors?: Record<string, string>;
}

export function extractErrorMessage(err: unknown, fallback: string): string {
  if (err instanceof HttpErrorResponse) {
    const body = err.error as ProblemDetail | null | undefined;
    if (body && typeof body === 'object') {
      if (body.errors && typeof body.errors === 'object') {
        const firstMsg = Object.values(body.errors)[0];
        if (firstMsg) {
          return firstMsg;
        }
      }
      if (body.detail) {
        return body.detail;
      }
      if (body.title) {
        return body.title;
      }
    }
    if (err.status === 0) {
      return 'No se pudo conectar con el servidor.';
    }
  }
  return fallback;
}
