import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { Loan } from '../../core/models/loan.model';
import { LoanService } from '../../core/services/loan.service';
import { UserLoansComponent } from './user-loans.component';

describe('UserLoansComponent', () => {
  let loanServiceMock: {
    loans: ReturnType<typeof signal<Loan[]>>;
    loading: ReturnType<typeof signal<boolean>>;
    error: ReturnType<typeof signal<string | null>>;
    loadMyLoans: jasmine.Spy;
    requestLoan: jasmine.Spy;
    cancelLoan: jasmine.Spy;
  };

  beforeEach(async () => {
    loanServiceMock = {
      loans: signal<Loan[]>([
        {
          id: 10,
          userId: 1,
          userEmail: 'usuario@test.com',
          amount: 1000,
          termMonths: 12,
          status: 'PENDING',
          createdAt: '2026-11-25T10:00:00Z',
          updatedAt: '2026-11-25T10:00:00Z',
        },
      ]),
      loading: signal(false),
      error: signal<string | null>(null),
      loadMyLoans: jasmine.createSpy('loadMyLoans').and.returnValue(of([])),
      requestLoan: jasmine.createSpy('requestLoan'),
      cancelLoan: jasmine.createSpy('cancelLoan'),
    };

    await TestBed.configureTestingModule({
      imports: [UserLoansComponent],
      providers: [
        provideRouter([]),
        { provide: LoanService, useValue: loanServiceMock },
        {
          provide: AuthService,
          useValue: {
            isAdmin: signal(false),
            currentUser: signal({
              id: 1,
              name: 'Usuario Demo',
              email: 'usuario@test.com',
              role: 'USER',
            }),
            logout: jasmine.createSpy('logout'),
          },
        },
      ],
    }).compileComponents();
  });

  it('rejects invalid loan form amounts and terms', () => {
    const fixture = TestBed.createComponent(UserLoansComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component['loanForm'].setValue({ amount: 0, termMonths: 500 });
    component['onSubmit']();

    expect(component['loanForm'].invalid).toBeTrue();
    expect(loanServiceMock.requestLoan).not.toHaveBeenCalled();
  });

  it('submits valid loan request and resets form', () => {
    const created: Loan = {
      id: 11,
      userId: 1,
      userEmail: 'usuario@test.com',
      amount: 2500,
      termMonths: 24,
      status: 'PENDING',
      createdAt: '2026-11-25T10:05:00Z',
      updatedAt: '2026-11-25T10:05:00Z',
    };
    loanServiceMock.requestLoan.and.returnValue(of(created));

    const fixture = TestBed.createComponent(UserLoansComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component['loanForm'].setValue({ amount: 2500, termMonths: 24 });
    component['onSubmit']();

    expect(loanServiceMock.requestLoan).toHaveBeenCalledWith({
      amount: 2500,
      termMonths: 24,
    });
    expect(component['successMessage']()).toContain('Solicitud #11');
    expect(component['loanForm'].controls.amount.value).toBeNull();
    expect(component['loanForm'].controls.termMonths.value).toBe(12);
  });

  it('cancels a pending loan and displays confirmation message', () => {
    const cancelled: Loan = {
      id: 10,
      userId: 1,
      userEmail: 'usuario@test.com',
      amount: 1000,
      termMonths: 12,
      status: 'CANCELLED',
      createdAt: '2026-11-25T10:00:00Z',
      updatedAt: '2026-11-25T10:10:00Z',
    };
    loanServiceMock.cancelLoan.and.returnValue(of(cancelled));

    const fixture = TestBed.createComponent(UserLoansComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.cancel(10);

    expect(loanServiceMock.cancelLoan).toHaveBeenCalledWith(10);
    expect(component['cancellingId']()).toBeNull();
    expect(component['successMessage']()).toContain('#10 cancelada');
  });
});
