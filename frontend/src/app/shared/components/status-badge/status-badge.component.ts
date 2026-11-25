import { Component, computed, input } from '@angular/core';
import { LoanStatus } from '../../../core/models/loan.model';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: `
    <span [class]="badgeClasses()">
      <span class="h-1.5 w-1.5 rounded-full" [class]="dotClass()"></span>
      {{ label() }}
    </span>
  `,
})
export class StatusBadgeComponent {
  readonly status = input.required<LoanStatus>();

  protected readonly label = computed(() => {
    switch (this.status()) {
      case 'APPROVED':
        return 'Aprobado';
      case 'REJECTED':
        return 'Rechazado';
      case 'CANCELLED':
        return 'Cancelado';
      default:
        return 'Pendiente';
    }
  });

  protected readonly badgeClasses = computed(() => {
    const base = 'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold border';
    switch (this.status()) {
      case 'APPROVED':
        return `${base} bg-emerald-50 text-emerald-700 border-emerald-200`;
      case 'REJECTED':
        return `${base} bg-rose-50 text-rose-700 border-rose-200`;
      case 'CANCELLED':
        return `${base} bg-slate-100 text-slate-600 border-slate-200`;
      default:
        return `${base} bg-amber-50 text-amber-700 border-amber-200`;
    }
  });

  protected readonly dotClass = computed(() => {
    switch (this.status()) {
      case 'APPROVED':
        return 'bg-emerald-500';
      case 'REJECTED':
        return 'bg-rose-500';
      case 'CANCELLED':
        return 'bg-slate-400';
      default:
        return 'bg-amber-500';
    }
  });
}
