import { CommonModule } from '@angular/common';
import { Component, Inject } from '@angular/core';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { UserRole } from '../../../../shared/models/user.model';

export interface RoleChangeDialogData {
  currentRole: UserRole;
  newRole: UserRole;
  userName: string;
}

@Component({
  selector: 'app-user-role-change-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule
  ],
  templateUrl: './user-role-change-dialog.component.html',
  styleUrls: ['./user-role-change-dialog.component.scss']
})
export class UserRoleChangeDialogComponent {
  constructor(
    private dialogRef: MatDialogRef<UserRoleChangeDialogComponent, boolean | null>,
    @Inject(MAT_DIALOG_DATA) public data: RoleChangeDialogData
  ) {}

  getRoleDisplayName(role: UserRole): string {
    return role === UserRole.ADMIN ? 'Admin' : 'Member';
  }

  isChangingToAdmin(): boolean {
    return this.data.newRole === UserRole.ADMIN;
  }

  isChangingToMember(): boolean {
    return this.data.newRole === UserRole.MEMBER;
  }

  cancel(): void {
    this.dialogRef.close(false);
  }

  confirm(): void {
    this.dialogRef.close(true);
  }
}
