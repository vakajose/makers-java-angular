import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { Loan } from '../../core/models/loan.model';
import { LoanService } from '../../core/services/loan.service';
import { AdminLoansComponent } from './admin-loans.component';

describe('AdminLoansComponent', () => {
  const sampleLoans: Loan[] = [
    {
      id: 1,
      userId: 1,
      userEmail: 'usuario@test.com',
      amount: 1000,
      termMonths: 12,
      status: 'PENDING',
      createdAt: '2026-11-25T10:00:00Z',
      updatedAt: '2026-11-25T10:00:00Z',
    },
    {
      id: 2,
      userId: 1,
      userEmail: 'usuario@test.com',
      amount: 2000,
      termMonths: 24,
      status: 'APPROVED',
      createdAt: '2026-11-25T10:01:00Z',
      updatedAt: '2026-11-25T10:02:00Z',
    },
    {
      id: 3,
      userId: 3,
      userEmail: 'cliente2@test.com',
      amount: 5000,
      termMonths: 36,
      status: 'REJECTED',
      createdAt: '2026-11-25T10:03:00Z',
      updatedAt: '2026-11-25T10:04:00Z',
    },
    {
      id: 4,
      userId: 3,
      userEmail: 'cliente2@test.com',
      amount: 300,
      termMonths: 6,
      status: 'CANCELLED',
      createdAt: '2026-11-25T10:05:00Z',
      updatedAt: '2026-11-25T10:06:00Z',
    },
  ];

  let loanServiceMock: {
    loans: ReturnType<typeof signal<Loan[]>>;
    loading: ReturnType<typeof signal<boolean>>;
    error: ReturnType<typeof signal<string | null>>;
    loadAllLoans: jasmine.Spy;
    approveLoan: jasmine.Spy;
    rejectLoan: jasmine.Spy;
  };

  beforeEach(async () => {
    loanServiceMock = {
      loans: signal<Loan[]>(sampleLoans),
      loading: signal(false),
      error: signal<string | null>(null),
      loadAllLoans: jasmine.createSpy('loadAllLoans').and.returnValue(of(sampleLoans)),
      approveLoan: jasmine.createSpy('approveLoan'),
      rejectLoan: jasmine.createSpy('rejectLoan'),
    };

    await TestBed.configureTestingModule({
      imports: [AdminLoansComponent],
      providers: [
        provideRouter([]),
        { provide: LoanService, useValue: loanServiceMock },
        {
          provide: AuthService,
          useValue: {
            isAdmin: signal(true),
            currentUser: signal({
              id: 2,
              name: 'Admin Admin',
              email: 'admin@test.com',
              role: 'ADMIN',
            }),
            logout: jasmine.createSpy('logout'),
          },
        },
      ],
    }).compileComponents();
  });

  it('loads loans on init and computes summary counters accurately', () => {
    const fixture = TestBed.createComponent(AdminLoansComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(loanServiceMock.loadAllLoans).toHaveBeenCalledTimes(1);
    expect(component['summary']()).toEqual({
      total: 4,
      pending: 1,
      approved: 1,
      rejected: 1,
      cancelled: 1,
    });
  });

  it('filters loans by status', () => {
    const fixture = TestBed.createComponent(AdminLoansComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component['filteredLoans']().length).toBe(4);

    component.setFilter('PENDING');
    expect(component['filteredLoans']().map((l) => l.id)).toEqual([1]);

    component.setFilter('CANCELLED');
    expect(component['filteredLoans']().map((l) => l.id)).toEqual([4]);

    component.setFilter('ALL');
    expect(component['filteredLoans']().length).toBe(4);
  });

  it('approves a pending loan and refreshes the list', () => {
    loanServiceMock.approveLoan.and.returnValue(
      of({ ...sampleLoans[0], status: 'APPROVED' })
    );

    const fixture = TestBed.createComponent(AdminLoansComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.approve(1);

    expect(loanServiceMock.approveLoan).toHaveBeenCalledWith(1);
    expect(component['processingId']()).toBeNull();
    expect(component['actionMessage']()).toBe('Préstamo #1 aprobado correctamente.');
    expect(loanServiceMock.loadAllLoans).toHaveBeenCalledTimes(2);
  });

  it('rejects a pending loan and refreshes preserving error if action fails', () => {
    loanServiceMock.rejectLoan.and.returnValue(of({ ...sampleLoans[0], status: 'REJECTED' }));

    const fixture = TestBed.createComponent(AdminLoansComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.reject(1);
    expect(loanServiceMock.rejectLoan).toHaveBeenCalledWith(1);
    expect(component['actionMessage']()).toBe('Préstamo #1 rechazado correctamente.');

    loanServiceMock.rejectLoan.and.returnValue(throwError(() => new Error('409')));
    component.reject(1);

    expect(component['processingId']()).toBeNull();
    expect(loanServiceMock.loadAllLoans).toHaveBeenCalledWith(true);
  });
});
