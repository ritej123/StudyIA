import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const noAuthGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) return true;
  
  const role = authService.userRole();
  return router.createUrlTree([role === 'ADMIN' ? '/admin/dashboard' : '/student/dashboard']);
};
