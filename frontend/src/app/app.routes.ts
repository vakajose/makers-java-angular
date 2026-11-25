import { Routes } from '@angular/router';
import { adminGuard } from './core/guards/admin.guard';
import { authGuard } from './core/guards/auth.guard';
import { AdminLoansComponent } from './features/admin-loans/admin-loans.component';
import { AdminUsersComponent } from './features/admin-users/admin-users.component';
import { LoginComponent } from './features/login/login.component';
import { UserLoansComponent } from './features/user-loans/user-loans.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: LoginComponent },
  { path: 'loans', component: UserLoansComponent, canActivate: [authGuard] },
  { path: 'admin', pathMatch: 'full', redirectTo: 'admin/loans' },
  { path: 'admin/loans', component: AdminLoansComponent, canActivate: [authGuard, adminGuard] },
  { path: 'admin/users', component: AdminUsersComponent, canActivate: [authGuard, adminGuard] },
  { path: '**', redirectTo: 'login' },
];
