export type LoanStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface Loan {
  id: number;
  userId: number;
  userEmail: string;
  amount: number;
  termMonths: number;
  status: LoanStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateLoanRequest {
  amount: number;
  termMonths: number;
}
