import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LoanService } from '../../core/services/loan.service';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-user-loans',
  standalone: true,
  imports: [ReactiveFormsModule, CurrencyPipe, DatePipe, NavbarComponent, StatusBadgeComponent],
  templateUrl: './user-loans.component.html',
})
export class UserLoansComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  protected readonly loanService = inject(LoanService);

  protected readonly successMessage = signal<string | null>(null);
  protected readonly cancellingId = signal<number | null>(null);

  protected readonly loanForm = this.fb.group({
    amount: [
      null as number | null,
      [Validators.required, Validators.min(1), Validators.max(1000000000)],
    ],
    termMonths: [
      12 as number | null,
      [Validators.required, Validators.min(1), Validators.max(360), Validators.pattern(/^\d+$/)],
    ],
  });

  protected readonly stats = computed(() => {
    const items = this.loanService.loans();
    const pending = items.filter((l) => l.status === 'PENDING').length;
    const approved = items.filter((l) => l.status === 'APPROVED');
    const approvedAmount = approved.reduce((acc, item) => acc + Number(item.amount), 0);
    return {
      total: items.length,
      pending,
      approvedCount: approved.length,
      approvedAmount,
    };
  });

  ngOnInit(): void {
    this.loanService.loadMyLoans().subscribe({ error: () => {} });
  }

  refresh(): void {
    this.successMessage.set(null);
    this.loanService.loadMyLoans().subscribe({ error: () => {} });
  }

  onSubmit(): void {
    if (this.loanForm.invalid) {
      this.loanForm.markAllAsTouched();
      return;
    }

    const amount = Number(this.loanForm.controls.amount.value);
    const termMonths = Number(this.loanForm.controls.termMonths.value);
    this.successMessage.set(null);

    this.loanService.requestLoan({ amount, termMonths }).subscribe({
      next: (created) => {
        this.successMessage.set(
          `Solicitud #${created.id} registrada por $${Number(created.amount).toLocaleString('en-US', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
          })} a ${created.termMonths} meses (Pendiente de aprobación).`
        );
        this.loanForm.reset({ amount: null, termMonths: 12 });
      },
      error: () => {},
    });
  }

  cancel(loanId: number): void {
    this.cancellingId.set(loanId);
    this.successMessage.set(null);

    this.loanService.cancelLoan(loanId).subscribe({
      next: () => {
        this.cancellingId.set(null);
        this.successMessage.set(`Solicitud #${loanId} cancelada correctamente.`);
      },
      error: () => {
        this.cancellingId.set(null);
      },
    });
  }

  estimatedMonthlyPayment(): number | null {
    const amount = Number(this.loanForm.controls.amount.value);
    const term = Number(this.loanForm.controls.termMonths.value);
    if (!amount || !term || amount <= 0 || term <= 0) {
      return null;
    }
    return amount / term;
  }
}
