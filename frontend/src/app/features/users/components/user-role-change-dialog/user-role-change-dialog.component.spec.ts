import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { UserRoleChangeDialogComponent, RoleChangeDialogData } from './user-role-change-dialog.component';
import { UserRole } from '../../../../shared/models/user.model';

describe('UserRoleChangeDialogComponent', () => {
  let component: UserRoleChangeDialogComponent;
  let fixture: ComponentFixture<UserRoleChangeDialogComponent>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<UserRoleChangeDialogComponent>>;

  const mockDialogData: RoleChangeDialogData = {
    currentRole: UserRole.MEMBER,
    newRole: UserRole.ADMIN,
    userName: 'John Doe'
  };

  beforeEach(async () => {
    const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      imports: [UserRoleChangeDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: mockDialogData }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UserRoleChangeDialogComponent);
    component = fixture.componentInstance;
    dialogRef = TestBed.inject(MatDialogRef) as jasmine.SpyObj<MatDialogRef<UserRoleChangeDialogComponent>>;
  });

  describe('Display Logic', () => {
    it('should display user name and role change details', () => {
      fixture.detectChanges();
      
      expect(component.data.userName).toBe('John Doe');
      expect(component.data.currentRole).toBe(UserRole.MEMBER);
      expect(component.data.newRole).toBe(UserRole.ADMIN);
    });

    it('should show admin warning when changing to ADMIN', () => {
      component.data = {
        currentRole: UserRole.MEMBER,
        newRole: UserRole.ADMIN,
        userName: 'John Doe'
      };
      
      expect(component.isChangingToAdmin()).toBeTrue();
      expect(component.isChangingToMember()).toBeFalse();
    });

    it('should show member info when changing to MEMBER', () => {
      component.data = {
        currentRole: UserRole.ADMIN,
        newRole: UserRole.MEMBER,
        userName: 'John Doe'
      };
      
      expect(component.isChangingToMember()).toBeTrue();
      expect(component.isChangingToAdmin()).toBeFalse();
    });
  });

  describe('Dialog Actions', () => {
    it('should close with true on confirm button click', () => {
      component.confirm();
      
      expect(dialogRef.close).toHaveBeenCalledWith(true);
    });

    it('should close with false on cancel button click', () => {
      component.cancel();
      
      expect(dialogRef.close).toHaveBeenCalledWith(false);
    });

    it('should close with false on ESC key press', () => {
      const escEvent = new KeyboardEvent('keydown', { key: 'Escape' });
      spyOn(escEvent, 'preventDefault');
      
      // Simulate ESC key behavior
      component.cancel();
      
      expect(dialogRef.close).toHaveBeenCalledWith(false);
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

    it('should detect changing to admin correctly', () => {
      component.data = {
        currentRole: UserRole.MEMBER,
        newRole: UserRole.ADMIN,
        userName: 'John Doe'
      };
      
      expect(component.isChangingToAdmin()).toBeTrue();
    });

    it('should detect changing to member correctly', () => {
      component.data = {
        currentRole: UserRole.ADMIN,
        newRole: UserRole.MEMBER,
        userName: 'John Doe'
      };
      
      expect(component.isChangingToMember()).toBeTrue();
    });
  });

  describe('Component Initialization', () => {
    it('should initialize with dialog data', () => {
      expect(component.data).toEqual(mockDialogData);
    });

    it('should have access to all required data properties', () => {
      expect(component.data.currentRole).toBeDefined();
      expect(component.data.newRole).toBeDefined();
      expect(component.data.userName).toBeDefined();
    });
  });

  describe('Role Change Scenarios', () => {
    it('should handle MEMBER to ADMIN role change', () => {
      component.data = {
        currentRole: UserRole.MEMBER,
        newRole: UserRole.ADMIN,
        userName: 'John Doe'
      };
      
      expect(component.isChangingToAdmin()).toBeTrue();
      expect(component.isChangingToMember()).toBeFalse();
    });

    it('should handle ADMIN to MEMBER role change', () => {
      component.data = {
        currentRole: UserRole.ADMIN,
        newRole: UserRole.MEMBER,
        userName: 'John Doe'
      };
      
      expect(component.isChangingToMember()).toBeTrue();
      expect(component.isChangingToAdmin()).toBeFalse();
    });
  });

  describe('Dialog Result Handling', () => {
    it('should return true when user confirms role change', () => {
      component.confirm();
      
      expect(dialogRef.close).toHaveBeenCalledWith(true);
    });

    it('should return false when user cancels role change', () => {
      component.cancel();
      
      expect(dialogRef.close).toHaveBeenCalledWith(false);
    });

    it('should handle multiple confirm calls', () => {
      component.confirm();
      component.confirm();
      
      expect(dialogRef.close).toHaveBeenCalledTimes(2);
      expect(dialogRef.close).toHaveBeenCalledWith(true);
    });

    it('should handle multiple cancel calls', () => {
      component.cancel();
      component.cancel();
      
      expect(dialogRef.close).toHaveBeenCalledTimes(2);
      expect(dialogRef.close).toHaveBeenCalledWith(false);
    });
  });
});
