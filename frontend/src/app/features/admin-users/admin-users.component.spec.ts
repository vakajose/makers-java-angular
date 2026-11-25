import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { UserProfile } from '../../core/models/auth.model';
import { UserService } from '../../core/services/user.service';
import { AdminUsersComponent } from './admin-users.component';

describe('AdminUsersComponent', () => {
  const initialUsers: UserProfile[] = [
    {
      id: 1,
      name: 'Usuario Demo',
      email: 'usuario@test.com',
      role: 'USER',
      createdAt: '2026-11-25T10:00:00Z',
    },
    {
      id: 2,
      name: 'Admin Admin',
      email: 'admin@test.com',
      role: 'ADMIN',
      createdAt: '2026-11-25T10:00:00Z',
    },
  ];

  let userServiceMock: {
    users: ReturnType<typeof signal<UserProfile[]>>;
    loading: ReturnType<typeof signal<boolean>>;
    error: ReturnType<typeof signal<string | null>>;
    loadUsers: jasmine.Spy;
    createUser: jasmine.Spy;
    updateUser: jasmine.Spy;
    deleteUser: jasmine.Spy;
    clearError: jasmine.Spy;
  };

  beforeEach(async () => {
    userServiceMock = {
      users: signal<UserProfile[]>(initialUsers),
      loading: signal(false),
      error: signal<string | null>(null),
      loadUsers: jasmine.createSpy('loadUsers').and.returnValue(of(initialUsers)),
      createUser: jasmine.createSpy('createUser'),
      updateUser: jasmine.createSpy('updateUser'),
      deleteUser: jasmine.createSpy('deleteUser'),
      clearError: jasmine.createSpy('clearError'),
    };

    await TestBed.configureTestingModule({
      imports: [AdminUsersComponent],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: userServiceMock },
        {
          provide: AuthService,
          useValue: {
            isAdmin: signal(true),
            currentUser: signal(initialUsers[1]),
            logout: jasmine.createSpy('logout'),
          },
        },
      ],
    }).compileComponents();
  });

  it('loads users on init and validates required password on creation', () => {
    const fixture = TestBed.createComponent(AdminUsersComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(userServiceMock.loadUsers).toHaveBeenCalledTimes(1);

    component['userForm'].setValue({
      name: 'Nuevo',
      email: 'nuevo@test.com',
      role: 'ADMIN',
      password: '',
    });
    component.onSubmit();

    expect(component['userForm'].invalid).toBeTrue();
    expect(userServiceMock.createUser).not.toHaveBeenCalled();
  });

  it('creates a new user or admin when form is valid', () => {
    const created: UserProfile = {
      id: 3,
      name: 'Segundo Admin',
      email: 'admin2@test.com',
      role: 'ADMIN',
    };
    userServiceMock.createUser.and.returnValue(of(created));

    const fixture = TestBed.createComponent(AdminUsersComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component['userForm'].setValue({
      name: 'Segundo Admin',
      email: 'admin2@test.com',
      role: 'ADMIN',
      password: '123',
    });
    component.onSubmit();

    expect(userServiceMock.createUser).toHaveBeenCalledWith({
      name: 'Segundo Admin',
      email: 'admin2@test.com',
      role: 'ADMIN',
      password: '123',
    });
    expect(component['successMessage']()).toContain('admin2@test.com (ADMIN)');
  });

  it('allows editing user without requiring password change', () => {
    const updated: UserProfile = {
      id: 1,
      name: 'Usuario Editado',
      email: 'usuario@test.com',
      role: 'USER',
    };
    userServiceMock.updateUser.and.returnValue(of(updated));

    const fixture = TestBed.createComponent(AdminUsersComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.startEdit(initialUsers[0]);
    component['userForm'].patchValue({ name: 'Usuario Editado' });
    component.onSubmit();

    expect(userServiceMock.updateUser).toHaveBeenCalledWith(1, {
      name: 'Usuario Editado',
      email: 'usuario@test.com',
      role: 'USER',
      password: null,
    });
    expect(component['editingUserId']()).toBeNull();
  });

  it('prevents deleting current session user and deletes other users', () => {
    userServiceMock.deleteUser.and.returnValue(of(void 0));

    const fixture = TestBed.createComponent(AdminUsersComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.deleteUser(initialUsers[1]);
    expect(userServiceMock.deleteUser).not.toHaveBeenCalled();

    component.deleteUser(initialUsers[0]);
    expect(userServiceMock.deleteUser).toHaveBeenCalledWith(1);
    expect(component['successMessage']()).toContain('usuario@test.com eliminado');
  });
});
