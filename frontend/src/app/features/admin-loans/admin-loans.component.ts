import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { LoanStatus } from '../../core/models/loan.model';
import { LoanService } from '../../core/services/loan.service';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';

type FilterStatus = 'ALL' | LoanStatus;

@Component({
  selector: 'app-admin-loans',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, NavbarComponent, StatusBadgeComponent],
  templateUrl: './admin-loans.component.html',
})
export class AdminLoansComponent implements OnInit {
  protected readonly loanService = inject(LoanService);

  protected readonly selectedFilter = signal<FilterStatus>('ALL');
  protected readonly processingId = signal<number | null>(null);
  protected readonly actionMessage = signal<string | null>(null);

  protected readonly filteredLoans = computed(() => {
    const filter = this.selectedFilter();
    const all = this.loanService.loans();
    return filter === 'ALL' ? all : all.filter((loan) => loan.status === filter);
  });

  protected readonly summary = computed(() => {
    const all = this.loanService.loans();
    return {
      total: all.length,
      pending: all.filter((l) => l.status === 'PENDING').length,
      approved: all.filter((l) => l.status === 'APPROVED').length,
      rejected: all.filter((l) => l.status === 'REJECTED').length,
      cancelled: all.filter((l) => l.status === 'CANCELLED').length,
    };
  });

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loanService.loadAllLoans().subscribe({ error: () => {} });
  }

  setFilter(filter: FilterStatus): void {
    this.selectedFilter.set(filter);
  }

  approve(loanId: number): void {
    this.processingId.set(loanId);
    this.actionMessage.set(null);

    this.loanService.approveLoan(loanId).subscribe({
      next: () => {
        this.processingId.set(null);
        this.actionMessage.set(`Préstamo #${loanId} aprobado correctamente.`);
        this.refresh();
      },
      error: () => {
        this.processingId.set(null);
        this.loanService.loadAllLoans(true).subscribe({ error: () => {} });
      },
    });
  }

  reject(loanId: number): void {
    this.processingId.set(loanId);
    this.actionMessage.set(null);

    this.loanService.rejectLoan(loanId).subscribe({
      next: () => {
        this.processingId.set(null);
        this.actionMessage.set(`Préstamo #${loanId} rechazado correctamente.`);
        this.refresh();
      },
      error: () => {
        this.processingId.set(null);
        this.loanService.loadAllLoans(true).subscribe({ error: () => {} });
      },
    });
  }
}
