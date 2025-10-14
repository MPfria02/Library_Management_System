import { ComponentFixture, fakeAsync, flushMicrotasks, TestBed, tick } from '@angular/core/testing';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { asyncScheduler, of, scheduled, throwError } from 'rxjs';
import { UserEditDialogComponent } from './user-edit-dialog.component';
import { UserService } from '../../../../shared/services/user.service';
import { UserResponse, UserRole } from '../../../../shared/models/user.model';
import { UserRoleChangeDialogComponent } from '../user-role-change-dialog/user-role-change-dialog.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('UserEditDialogComponent', () => {
  let component: UserEditDialogComponent;
  let fixture: ComponentFixture<UserEditDialogComponent>;
  let userService: jasmine.SpyObj<UserService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<UserEditDialogComponent>>;

  const mockUser: UserResponse = {
    id: 1,
    email: 'john.doe@example.com',
    firstName: 'John',
    lastName: 'Doe',
    phone: '123-456-7890',
    role: UserRole.MEMBER
  };

  beforeEach(async () => {
    userService = jasmine.createSpyObj('UserService', ['update']);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close', 'afterClosed']);
    dialog = jasmine.createSpyObj('MatDialog', ['open'])

    await TestBed.configureTestingModule({
      imports: [
        UserEditDialogComponent,
        ReactiveFormsModule,
        NoopAnimationsModule  // prevents animation-related async issues
      ],
      providers: [
        { provide: UserService, useValue: userService },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: mockUser },
      ]
    })
    .overrideProvider(MatSnackBar, { useValue: snackBar })
    .overrideProvider(MatDialog, { useValue: dialog })
    .compileComponents();

    fixture = TestBed.createComponent(UserEditDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('Form Initialization', () => {
    it('should initialize form with user data', () => {
      component.ngOnInit();
      
      expect(component.form.get('firstName')?.value).toBe(mockUser.firstName);
      expect(component.form.get('lastName')?.value).toBe(mockUser.lastName);
      expect(component.form.get('phone')?.value).toBe(mockUser.phone);
      expect(component.form.get('role')?.value).toBe(mockUser.role);
    });

    it('should create form with correct validators', () => {
      component.ngOnInit();
      
      const firstNameControl = component.form.get('firstName');
      const lastNameControl = component.form.get('lastName');
      const phoneControl = component.form.get('phone');
      const roleControl = component.form.get('role');
      
      expect(firstNameControl?.hasError('required')).toBeFalse();
      expect(lastNameControl?.hasError('required')).toBeFalse();
      expect(phoneControl?.hasError('required')).toBeFalse();
      expect(roleControl?.hasError('required')).toBeFalse();
    });
  });

  describe('Field Validation', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should mark firstName as invalid when empty', () => {
      component.form.get('firstName')?.setValue('');
      component.form.get('firstName')?.markAsTouched();
      
      expect(component.form.get('firstName')?.hasError('required')).toBeTrue();
      expect(component.getFieldError('firstName')).toBe('First name is required');
    });

    it('should mark firstName as invalid when exceeds 50 chars', () => {
      const longName = 'a'.repeat(51);
      component.form.get('firstName')?.setValue(longName);
      component.form.get('firstName')?.markAsTouched();
      
      expect(component.form.get('firstName')?.hasError('maxlength')).toBeTrue();
      expect(component.getFieldError('firstName')).toBe('Max 50 characters');
    });

    it('should mark lastName as invalid when empty', () => {
      component.form.get('lastName')?.setValue('');
      component.form.get('lastName')?.markAsTouched();
      
      expect(component.form.get('lastName')?.hasError('required')).toBeTrue();
      expect(component.getFieldError('lastName')).toBe('Last name is required');
    });

    it('should mark phone as invalid when empty', () => {
      component.form.get('phone')?.setValue('');
      component.form.get('phone')?.markAsTouched();
      
      expect(component.form.get('phone')?.hasError('required')).toBeTrue();
      expect(component.getFieldError('phone')).toBe('Phone is required');
    });

    it('should mark phone as invalid with wrong format', () => {
      component.form.get('phone')?.setValue('123456789');
      component.form.get('phone')?.markAsTouched();
      
      expect(component.form.get('phone')?.hasError('invalidPhone')).toBeTrue();
      expect(component.getFieldError('phone')).toBe('Invalid phone format');
    });
  });

  describe('Role Change Flow', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should open role change dialog when role changes', () => {
      dialogRef.afterClosed.and.returnValue(of(true));
      dialog.open.and.returnValue(dialogRef);
      
      component.form.get('role')?.setValue(UserRole.ADMIN);
      
      expect(dialog.open).toHaveBeenCalledWith(UserRoleChangeDialogComponent, {
        width: '450px',
        data: {
          currentRole: mockUser.role,
          newRole: UserRole.ADMIN,
          userName: `${mockUser.firstName} ${mockUser.lastName}`
        },
        disableClose: true
      });
    });

    it('should revert role when confirmation cancelled', () => {
      dialogRef.afterClosed.and.returnValue(of(false));
      dialog.open.and.returnValue(dialogRef);
      
      component.form.get('role')?.setValue(UserRole.ADMIN);
      
      expect(component.form.get('role')?.value).toBe(mockUser.role);
    });

    it('should keep new role when confirmation accepted', () => {
      dialogRef.afterClosed.and.returnValue(of(true));
      dialog.open.and.returnValue(dialogRef);
      
      component.form.get('role')?.setValue(UserRole.ADMIN);
      
      expect(component.form.get('role')?.value).toBe(UserRole.ADMIN);
    });
  });

  describe('Form Submission', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should disable save button when form invalid', () => {
      component.form.get('firstName')?.setValue('');
      component.form.markAllAsTouched();
      
      expect(component.form.invalid).toBeTrue();
    });

    it('should disable save button when form unchanged', () => {
      expect(component.isFormUnchanged()).toBeTrue();
    });

    it('should call userService.update on valid submission', () => {
      const updatedUser = { ...mockUser, firstName: 'Jane' };
      userService.update.and.returnValue(of(updatedUser));
      
      component.form.get('firstName')?.setValue('Jane');
      component.save();
      
      expect(userService.update).toHaveBeenCalledWith(mockUser.id, jasmine.objectContaining({
        firstName: 'Jane',
        lastName: mockUser.lastName,
        phone: mockUser.phone,
        role: mockUser.role
      }));
    });
  });

  describe('Success/Error Handling', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should inject correct SnackBar mock instance', () => {
      console.log('SnackBar instance:', component['snackBar']);
      console.log('SnackBar spy:', snackBar);
      console.log('Same?', component['snackBar'] === snackBar);

      console.log('UserService instance:', component['userService']);
      console.log('UserService spy:', userService);
      console.log('Same?', component['userService'] === userService);
    });

    it('should close dialog and show success message on save', fakeAsync(() => {
      const updatedUser = { ...mockUser, firstName: 'Jane' };
      userService.update.and.returnValue(of(updatedUser));

      component.form.get('firstName')?.setValue('Jane');
      fixture.detectChanges();
      component.save();
      tick();
      console.log('Form valid:', component.form.valid);
      console.log('UserService called:', userService.update.calls.any());
      
      expect(userService.update).toHaveBeenCalled();
      expect(snackBar.open).toHaveBeenCalledWith(
        'User updated successfully',
        'Close',
        { duration: 2500 }
      );
      console.log(snackBar.open.calls.any());
      expect(dialogRef.close).toHaveBeenCalledWith(updatedUser);
    }));

    it('should show error snackbar on save failure', () => {
      userService.update.and.returnValue(throwError(() => new Error('Update failed')));
      
      component.form.get('firstName')?.setValue('Jane');
      component.save();
      console.log('Form valid:', component.form.valid);
      console.log('UserService called:', userService.update.calls.any());
      

      expect(snackBar.open).toHaveBeenCalledWith(
        'Failed to update user. Please try again.',
        'Close',
        { duration: 3000 }
      );
      console.log(snackBar.open.calls.any());
    });
  });

  describe('Phone Input Formatting', () => {
    beforeEach(() => {
      component.ngOnInit();
    });

    it('should format phone input with dashes', () => {
      const inputEvent = {
        target: { value: '1234567890' }
      } as any;
      
      component.onPhoneInput(inputEvent);
      
      expect(component.form.get('phone')?.value).toBe('123-456-7890');
    });

    it('should handle phone keydown events', () => {
      const keydownEvent = {
        key: '1',
        ctrlKey: false,
        target: { value: '123' },
        preventDefault: jasmine.createSpy('preventDefault')
      } as any;
      
      component.onPhoneKeyDown(keydownEvent);
      
      expect(keydownEvent.preventDefault).not.toHaveBeenCalled();
    });

    it('should prevent input if already 10 digits', () => {
      const event = {
        key: '5',
        ctrlKey: false,
        preventDefault: jasmine.createSpy('preventDefault'),
        target: { value: '1234567890' }
      } as any;
    
      component.onPhoneKeyDown(event);
    
      expect(event.preventDefault).toHaveBeenCalled();
    });
    

    it('should handle phone paste events', () => {
      const pasteEvent = {
        clipboardData: {
          getData: jasmine.createSpy('getData').and.returnValue('1234567890')
        },
        preventDefault: jasmine.createSpy('preventDefault')
      } as any;
      
      component.onPhonePaste(pasteEvent);
      
      expect(component.form.get('phone')?.value).toBe('123-456-7890');
    });
  });

  describe('Helper Methods', () => {
    it('should return correct role display name for ADMIN', () => {
      const displayName = component.getRoleDisplayName(UserRole.ADMIN);
      expect(displayName).toBe('Admin');
    });

    it('should return correct role display name for MEMBER', () => {
      const displayName = component.getRoleDisplayName(UserRole.MEMBER);
      expect(displayName).toBe('Member');
    });

    it('should return correct field labels through getFieldError', () => {
      component.form.get('firstName')?.setValue('');
      component.form.get('firstName')?.markAsTouched();
      
      const error = component.getFieldError('firstName');
      expect(error).toContain('First name');
    });
  });

  describe('Cancel Action', () => {
    it('should close dialog without data on cancel', () => {
      component.cancel();
      
      expect(dialogRef.close).toHaveBeenCalledWith(null);
    });
  });

  describe('Loading States', () => {
    it('should show loading state during submission', fakeAsync(() => {
      // schedule emission asynchronously
      userService.update.and.returnValue(scheduled([mockUser], asyncScheduler));

      component.form.get('firstName')?.setValue('Jane');
      fixture.detectChanges();

      component.save();

      // Right after calling save(), the submitting flag should be true
      expect(component.submitting()).toBeTrue();

      // Flush the scheduled async emission
      tick(); 

      // After the "response", it should be false again
      expect(component.submitting()).toBeFalse();
        
    }));
  });
});
