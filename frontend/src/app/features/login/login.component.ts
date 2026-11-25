import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { extractErrorMessage } from '../../core/models/problem-detail.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './login.component.html',
})
export class LoginComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.redirectByRole();
    }
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    const email = this.loginForm.controls.email.value ?? '';
    const password = this.loginForm.controls.password.value ?? '';

    this.submitting.set(true);
    this.errorMessage.set(null);

    this.authService.login({ email, password }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.redirectByRole();
      },
      error: (err) => {
        this.submitting.set(false);
        this.errorMessage.set(
          extractErrorMessage(err, 'Credenciales inválidas. Verifica tu correo y contraseña.')
        );
      },
    });
  }

  fillDemoCredentials(email: string, password: string): void {
    this.loginForm.patchValue({ email, password });
    this.errorMessage.set(null);
  }

  private redirectByRole(): void {
    const target = this.authService.isAdmin() ? '/admin/loans' : '/loans';
    void this.router.navigate([target]);
  }
}
