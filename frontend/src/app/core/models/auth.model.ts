export type Role = 'USER' | 'ADMIN';

export interface UserProfile {
  id: number;
  name: string;
  email: string;
  role: Role;
  createdAt?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  user: UserProfile;
}
