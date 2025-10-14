import { CommonModule } from '@angular/common';
import { Component, Inject, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatDialogModule, MatDialog , MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { UserResponse, UserRole } from '../../../../shared/models/user.model';
import { UserService } from '../../../../shared/services/user.service';
import { UserRoleChangeDialogComponent } from '../user-role-change-dialog/user-role-change-dialog.component';

@Component({
  selector: 'app-user-edit-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatCardModule
  ],
  templateUrl: './user-edit-dialog.component.html',
  styleUrls: ['./user-edit-dialog.component.scss']
})
export class UserEditDialogComponent implements OnInit {
  form!: FormGroup;
  originalUser!: UserResponse;
  submitting = signal<boolean>(false);
  readonly userRoles = Object.values(UserRole);

  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);

  constructor(
    private dialogRef: MatDialogRef<UserEditDialogComponent, UserResponse | null>,
    @Inject(MAT_DIALOG_DATA) public data: UserResponse
  ) {
    this.originalUser = { ...data };
  }

  ngOnInit(): void {
    this.form = this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(50)]],
      lastName: ['', [Validators.required, Validators.maxLength(50)]],
      phone: ['', [Validators.required, this.phoneValidator]],
      role: [this.data.role, [Validators.required]]
    });

    // Set initial values
    this.form.patchValue({
      firstName: this.data.firstName,
      lastName: this.data.lastName,
      phone: this.data.phone,
      role: this.data.role
    });

    // Watch for role changes
    this.form.get('role')?.valueChanges.subscribe((newRole: UserRole) => {
      if (newRole !== this.originalUser.role) {
        this.handleRoleChange(newRole);
      }
    });
  }

  private phoneValidator(control: AbstractControl): ValidationErrors | null {
    const phonePattern = /^\d{3}-\d{3}-\d{4}$/;
    if (control.value && !phonePattern.test(control.value)) {
      return { invalidPhone: true };
    }
    return null;
  }

  onPhoneInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/\D/g, ''); // Remove all non-digits
    
    // Limit to 10 digits
    if (value.length > 10) {
      value = value.substring(0, 10);
    }
    
    // Format with dashes
    if (value.length >= 6) {
      value = `${value.substring(0, 3)}-${value.substring(3, 6)}-${value.substring(6)}`;
    } else if (value.length >= 3) {
      value = `${value.substring(0, 3)}-${value.substring(3)}`;
    }
    
    // Update the form control value
    this.form.get('phone')?.setValue(value, { emitEvent: false });
  }

  onPhoneKeyDown(event: KeyboardEvent): void {
    const input = event.target as HTMLInputElement;
    const value = input.value.replace(/\D/g, '');
    
    // Allow navigation and control keys
    const allowedKeys = [
      'Backspace', 'Delete', 'Tab', 'Escape', 'Enter',
      'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown',
      'Home', 'End'
    ];
    
    // Allow Ctrl+A, Ctrl+C, Ctrl+V, Ctrl+X
    if (event.ctrlKey && ['a', 'c', 'v', 'x'].includes(event.key.toLowerCase())) {
      return;
    }
    
    // Allow navigation and control keys
    if (allowedKeys.includes(event.key)) {
      return;
    }
    
    // Allow only digits (0-9)
    if (!/^[0-9]$/.test(event.key)) {
      event.preventDefault();
      return;
    }
    
    // Prevent input if already 10 digits
    if (value.length >= 10) {
      event.preventDefault();
    }
  }

  onPhonePaste(event: ClipboardEvent): void {
    event.preventDefault();
    const clipboardData = event.clipboardData?.getData('text') || '';
    const digits = clipboardData.replace(/\D/g, '');
    
    if (digits.length > 0) {
      // Limit to 10 digits
      const limitedDigits = digits.substring(0, 10);
      
      // Format with dashes
      let formattedValue = limitedDigits;
      if (limitedDigits.length >= 6) {
        formattedValue = `${limitedDigits.substring(0, 3)}-${limitedDigits.substring(3, 6)}-${limitedDigits.substring(6)}`;
      } else if (limitedDigits.length >= 3) {
        formattedValue = `${limitedDigits.substring(0, 3)}-${limitedDigits.substring(3)}`;
      }
      
      this.form.get('phone')?.setValue(formattedValue);
    }
  }

  private handleRoleChange(newRole: UserRole): void {
    const dialogRef = this.dialog.open(UserRoleChangeDialogComponent, {
      width: '450px',
      data: {
        currentRole: this.originalUser.role,
        newRole: newRole,
        userName: `${this.originalUser.firstName} ${this.originalUser.lastName}`
      },
      disableClose: true
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean | null) => {
      if (confirmed) {
        // Keep the new role
        this.form.get('role')?.setValue(newRole, { emitEvent: false });
      } else {
        // Revert to original role
        this.form.get('role')?.setValue(this.originalUser.role, { emitEvent: false });
      }
    });
  }

  getFieldError(fieldName: string): string {
    const field = this.form.get(fieldName);
    if (field?.errors && field.touched) {
      if (field.errors['required']) {
        return `${this.getFieldLabel(fieldName)} is required`;
      }
      if (field.errors['maxlength']) {
        return `Max ${field.errors['maxlength'].requiredLength} characters`;
      }
      if (field.errors['invalidPhone']) {
        return 'Invalid phone format';
      }
    }
    return '';
  }

  private getFieldLabel(fieldName: string): string {
    const labels: Record<string, string> = {
      firstName: 'First name',
      lastName: 'Last name',
      phone: 'Phone',
      role: 'Role'
    };
    return labels[fieldName] || fieldName;
  }

  getRoleDisplayName(role: UserRole): string {
    return role === UserRole.ADMIN ? 'Admin' : 'Member';
  }

  isFormUnchanged(): boolean {
    const formValue = this.form.value;
    return (
      formValue.firstName === this.originalUser.firstName &&
      formValue.lastName === this.originalUser.lastName &&
      formValue.phone === this.originalUser.phone &&
      formValue.role === this.originalUser.role
    );
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (this.isFormUnchanged()) {
      this.snackBar.open('No changes to save', 'Close', { duration: 2000 });
      return;
    }

    this.submitting.set(true);
    const formValue = this.form.value;

    const updatedUser: UserResponse = {
      id: this.data.id,
      email: this.data.email, // Email is not editable
      firstName: formValue.firstName,
      lastName: formValue.lastName,
      phone: formValue.phone,
      role: formValue.role
    };

    this.userService.update(this.data.id, updatedUser).subscribe({
      next: (result: UserResponse) => {
        this.submitting.set(false);
        this.snackBar.open('User updated successfully', 'Close', { duration: 2500 });
        this.dialogRef.close(result);
      },
      error: (err: unknown) => {
        console.error('Failed to update user', err);
        this.submitting.set(false);
        this.snackBar.open('Failed to update user. Please try again.', 'Close', { duration: 3000 });
      }
    });
  }
}
