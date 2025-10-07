import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../auth.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const snackBar = inject(MatSnackBar);

  if (authService.isLoggedIn()) {
    return true;
  } 

  // If we reach here, user is not logged in
  snackBar.open('Please login to access this page', 'Close', { duration: 3000 });
  router.navigate(['/login']);
  return false;
}
