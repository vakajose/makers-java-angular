import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="border-b border-slate-200 bg-white">
      <div class="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-4 py-3.5 sm:px-6">
        <div class="flex items-center gap-6">
          <div class="flex items-center gap-3">
            <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-900 text-sm font-bold text-white">
              M
            </div>
            <div>
              <p class="text-sm font-semibold text-slate-900">Makers Bank</p>
              <p class="text-xs text-slate-500">
                @if (auth.isAdmin()) {
                  Panel de Administración
                } @else {
                  Portal de Préstamos
                }
              </p>
            </div>
          </div>

          @if (auth.isAdmin()) {
            <nav class="flex items-center gap-1 rounded-lg bg-slate-100 p-1">
              <a
                routerLink="/admin/loans"
                routerLinkActive="bg-white text-slate-900 shadow-xs"
                class="rounded-md px-3 py-1.5 text-xs font-medium text-slate-600 transition hover:text-slate-900"
              >
                Préstamos
              </a>
              <a
                routerLink="/admin/users"
                routerLinkActive="bg-white text-slate-900 shadow-xs"
                class="rounded-md px-3 py-1.5 text-xs font-medium text-slate-600 transition hover:text-slate-900"
              >
                Usuarios
              </a>
            </nav>
          }
        </div>

        @if (auth.currentUser(); as user) {
          <div class="flex items-center gap-4">
            <div class="hidden text-right sm:block">
              <p class="text-sm font-medium text-slate-900">{{ user.name }}</p>
              <p class="text-xs text-slate-500">{{ user.email }} · {{ user.role }}</p>
            </div>
            <button
              type="button"
              (click)="logout()"
              class="rounded-lg border border-slate-300 bg-white px-3.5 py-2 text-xs font-medium text-slate-700 transition hover:bg-slate-50 hover:text-slate-900"
            >
              Cerrar Sesión
            </button>
          </div>
        }
      </div>
    </header>
  `,
})
export class NavbarComponent {
  protected readonly auth = inject(AuthService);

  logout(): void {
    this.auth.logout();
  }
}
