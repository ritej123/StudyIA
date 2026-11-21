export type UserRole = 'ETUDIANT' | 'ADMIN';
export type UserStatus = 'ACTIF' | 'DESACTIVE' | 'EN_ATTENTE';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: UserRole;
  status: UserStatus;
  photoUrl?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AdminUser extends User {}
