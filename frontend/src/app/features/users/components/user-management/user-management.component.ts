import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCardModule } from '@angular/material/card';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router } from '@angular/router';
import { UserResponse, UserRole } from '../../../../shared/models/user.model';
import { UserService } from '../../../../shared/services/user.service';
import { UserEditDialogComponent } from '../user-edit-dialog/user-edit-dialog.component';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatIconModule,
    MatButtonModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatCardModule,
    MatToolbarModule
  ],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss']
})
export class UserManagementComponent implements OnInit {
  users = signal<UserResponse[]>([]);
  loading = signal<boolean>(false);
  searchFirstName = signal<string>('');
  searchLastName = signal<string>('');

  readonly displayedColumns = ['email', 'firstName', 'lastName', 'phone', 'role', 'actions'];

  private userService = inject(UserService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.userService.getMembers().subscribe({
      next: (users: UserResponse[]) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        console.error('Failed to load users', err);
        this.loading.set(false);
        this.snackBar.open('Failed to load users. Please try again.', 'Close', { duration: 3000 });
      }
    });
  }

  searchUsers(): void {
    const firstName = this.searchFirstName();
    const lastName = this.searchLastName();

    // If both fields are empty, load all members
    if (!firstName && !lastName) {
      this.loadUsers();
      return;
    }

    this.loading.set(true);
    this.userService.search(firstName || undefined, lastName || undefined).subscribe({
      next: (users: UserResponse[]) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        console.error('Failed to search users', err);
        this.loading.set(false);
        this.snackBar.open('Failed to search users. Please try again.', 'Close', { duration: 3000 });
      }
    });
  }

  clearSearch(): void {
    this.searchFirstName.set('');
    this.searchLastName.set('');
    this.loadUsers();
  }

  editUser(user: UserResponse): void {
    const dialogRef = this.dialog.open(UserEditDialogComponent, {
      width: '500px',
      data: user,
      disableClose: false
    });

    dialogRef.afterClosed().subscribe((updatedUser: UserResponse | null) => {
      if (updatedUser) {
        // Reload user list to show updated data
        this.loadUsers();
      }
    });
  }

  goBackToBooks(): void {
    this.router.navigate(['/admin/books']);
  }

  getRoleColor(role: UserRole): string {
    return role === UserRole.ADMIN ? 'primary' : 'accent';
  }

  getRoleDisplayName(role: UserRole): string {
    return role === UserRole.ADMIN ? 'Admin' : 'Member';
  }
}
