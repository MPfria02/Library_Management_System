import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../auth.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const snackBar = inject(MatSnackBar);

  if (!authService.isLoggedIn()) {
    snackBar.open('Please login to access this page', 'Close', { duration: 3000 });
    router.navigate(['/login']);
    return false;
  }

  if (authService.getUserRole() === 'ADMIN') {
    return true;
  } else {
    snackBar.open('Access denied. Admin privileges required', 'Close', { duration: 3000 });
    router.navigate(['/dashboard']);
    return false;
  }
};
