import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';
import { Role, UserProfile } from '../../core/models/auth.model';
import { UserService } from '../../core/services/user.service';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, NavbarComponent],
  templateUrl: './admin-users.component.html',
})
export class AdminUsersComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  protected readonly authService = inject(AuthService);
  protected readonly userService = inject(UserService);

  protected readonly editingUserId = signal<number | null>(null);
  protected readonly submitting = signal(false);
  protected readonly deletingId = signal<number | null>(null);
  protected readonly successMessage = signal<string | null>(null);

  protected readonly userForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(180)]],
    role: ['USER' as Role, [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
  });

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.userService.loadUsers().subscribe({ error: () => {} });
  }

  startEdit(user: UserProfile): void {
    this.editingUserId.set(user.id);
    this.successMessage.set(null);
    this.userService.clearError();

    this.userForm.controls.password.setValidators([
      Validators.minLength(3),
      Validators.maxLength(100),
    ]);
    this.userForm.controls.password.updateValueAndValidity();

    this.userForm.reset({
      name: user.name,
      email: user.email,
      role: user.role,
      password: '',
    });
  }

  cancelEdit(): void {
    this.editingUserId.set(null);
    this.userService.clearError();

    this.userForm.controls.password.setValidators([
      Validators.required,
      Validators.minLength(3),
      Validators.maxLength(100),
    ]);
    this.userForm.controls.password.updateValueAndValidity();

    this.userForm.reset({
      name: '',
      email: '',
      role: 'USER',
      password: '',
    });
  }

  onSubmit(): void {
    if (this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      return;
    }

    const name = (this.userForm.controls.name.value ?? '').trim();
    const email = (this.userForm.controls.email.value ?? '').trim();
    const role = (this.userForm.controls.role.value ?? 'USER') as Role;
    const password = this.userForm.controls.password.value ?? '';

    this.submitting.set(true);
    this.successMessage.set(null);

    const targetId = this.editingUserId();
    if (targetId === null) {
      this.userService.createUser({ name, email, password, role }).subscribe({
        next: (created) => {
          this.submitting.set(false);
          this.successMessage.set(`Usuario ${created.email} (${created.role}) creado correctamente.`);
          this.cancelEdit();
        },
        error: () => {
          this.submitting.set(false);
        },
      });
      return;
    }

    this.userService
      .updateUser(targetId, {
        name,
        email,
        role,
        password: password.trim() ? password : null,
      })
      .subscribe({
        next: (updated) => {
          this.submitting.set(false);
          this.successMessage.set(`Usuario ${updated.email} actualizado correctamente.`);
          this.cancelEdit();
        },
        error: () => {
          this.submitting.set(false);
        },
      });
  }

  deleteUser(user: UserProfile): void {
    if (this.isCurrentSessionUser(user.id)) {
      return;
    }
    this.deletingId.set(user.id);
    this.successMessage.set(null);

    this.userService.deleteUser(user.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        if (this.editingUserId() === user.id) {
          this.cancelEdit();
        }
        this.successMessage.set(`Usuario ${user.email} eliminado correctamente.`);
      },
      error: () => {
        this.deletingId.set(null);
      },
    });
  }

  isCurrentSessionUser(userId: number): boolean {
    return this.authService.currentUser()?.id === userId;
  }
}
