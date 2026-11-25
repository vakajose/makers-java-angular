import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth/auth.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let authServiceMock: {
    isAuthenticated: ReturnType<typeof signal<boolean>>;
    isAdmin: ReturnType<typeof signal<boolean>>;
    login: jasmine.Spy;
  };
  let router: Router;

  beforeEach(async () => {
    authServiceMock = {
      isAuthenticated: signal(false),
      isAdmin: signal(false),
      login: jasmine.createSpy('login'),
    };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  });

  it('marks invalid form without calling login', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component['onSubmit']();

    expect(component['loginForm'].invalid).toBeTrue();
    expect(authServiceMock.login).not.toHaveBeenCalled();
  });

  it('redirects to /admin/loans when admin logs in', () => {
    authServiceMock.login.and.callFake(() => {
      authServiceMock.isAdmin.set(true);
      return of({
        token: 'jwt',
        user: { id: 2, name: 'Admin', email: 'admin@test.com', role: 'ADMIN' },
      });
    });

    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component['fillDemoCredentials']('admin@test.com', '123');
    component['onSubmit']();

    expect(authServiceMock.login).toHaveBeenCalledWith({
      email: 'admin@test.com',
      password: '123',
    });
    expect(router.navigate).toHaveBeenCalledWith(['/admin/loans']);
  });

  it('shows server error message when login fails', () => {
    authServiceMock.login.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 401,
            error: { detail: 'Credenciales inválidas.' },
          })
      )
    );

    const fixture = TestBed.createComponent(LoginComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component['fillDemoCredentials']('usuario@test.com', 'wrong');
    component['onSubmit']();

    expect(component['errorMessage']()).toBe('Credenciales inválidas.');
  });
});
