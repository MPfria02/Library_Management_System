import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { asyncScheduler, of, scheduled, throwError } from 'rxjs';
import { UserManagementComponent } from './user-management.component';
import { UserService } from '../../../../shared/services/user.service';
import { UserResponse, UserRole } from '../../../../shared/models/user.model';
import { UserEditDialogComponent } from '../user-edit-dialog/user-edit-dialog.component';

describe('UserManagementComponent', () => {
  let component: UserManagementComponent;
  let fixture: ComponentFixture<UserManagementComponent>;
  let userService: jasmine.SpyObj<UserService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let router: jasmine.SpyObj<Router>;

  const mockUsers: UserResponse[] = [
    {
      id: 1,
      email: 'john.doe@example.com',
      firstName: 'John',
      lastName: 'Doe',
      phone: '123-456-7890',
      role: UserRole.MEMBER
    },
    {
      id: 2,
      email: 'admin@example.com',
      firstName: 'Admin',
      lastName: 'User',
      phone: '987-654-3210',
      role: UserRole.ADMIN
    }
  ];

  beforeEach(async () => {
    const userServiceSpy = jasmine.createSpyObj('UserService', ['getMembers', 'search']);
    const dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [UserManagementComponent],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: Router, useValue: routerSpy },
      ]
    })
    .overrideProvider(MatDialog, { useValue: dialogSpy })
    .overrideProvider(MatSnackBar, { useValue: snackBarSpy })
    .compileComponents();

    fixture = TestBed.createComponent(UserManagementComponent);
    component = fixture.componentInstance;
    userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;
    dialog = TestBed.inject(MatDialog) as jasmine.SpyObj<MatDialog>;
    snackBar = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  describe('Initialization', () => {
    it('should load users on init', () => {
      userService.getMembers.and.returnValue(of(mockUsers));
      
      component.ngOnInit();
      
      expect(userService.getMembers).toHaveBeenCalled();
      expect(component.users()).toEqual(mockUsers);
      expect(component.loading()).toBeFalse();
    });

    it('should show loading state during API call', () => {
      userService.getMembers.and.returnValue(of(mockUsers));
      
      component.ngOnInit();
      
      expect(component.loading()).toBeFalse();
    });
  });

  describe('Search Functionality', () => {
    beforeEach(() => {
      userService.getMembers.and.returnValue(of(mockUsers));
      userService.search.and.returnValue(of(mockUsers));
      component.ngOnInit();
    });

    it('should search users with firstName and lastName', () => {
      component.searchFirstName.set('John');
      component.searchLastName.set('Doe');
      
      component.searchUsers();
      
      expect(userService.search).toHaveBeenCalledWith('John', 'Doe');
      expect(component.users()).toEqual(mockUsers);
    });

    it('should call getMembers when both search fields empty', () => {
      component.searchFirstName.set('');
      component.searchLastName.set('');
      
      component.searchUsers();
      
      expect(userService.getMembers).toHaveBeenCalled();
      expect(userService.search).not.toHaveBeenCalled();
    });

    it('should update users signal with search results', () => {
      const searchResults = [mockUsers[0]];
      userService.search.and.returnValue(of(searchResults));
      
      component.searchFirstName.set('John');
      component.searchUsers();
      
      expect(component.users()).toEqual(searchResults);
    });
  });

  describe('Edit Action', () => {
    beforeEach(() => {
      userService.getMembers.and.returnValue(of(mockUsers));
      component.ngOnInit();
    });

    it('should open UserEditDialogComponent on edit button click', () => {
      const mockDialogRef = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
      mockDialogRef.afterClosed.and.returnValue(of(null));
      dialog.open.and.returnValue(mockDialogRef);
      
      component.editUser(mockUsers[0]);
      
      expect(dialog.open).toHaveBeenCalledWith(UserEditDialogComponent, {
        width: '500px',
        data: mockUsers[0],
        disableClose: false
      });
    });

    it('should reload users after successful edit', () => {
      const updatedUser = { ...mockUsers[0], firstName: 'Jane' };
      const mockDialogRef = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
      mockDialogRef.afterClosed.and.returnValue(of(updatedUser));
      dialog.open.and.returnValue(mockDialogRef);
      userService.getMembers.and.returnValue(of(mockUsers));
      
      component.editUser(mockUsers[0]);
      
      expect(userService.getMembers).toHaveBeenCalledTimes(2); // Once in ngOnInit, once after edit
    });

    it('should not reload when edit dialog cancelled', () => {
      const mockDialogRef = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
      mockDialogRef.afterClosed.and.returnValue(of(null));
      dialog.open.and.returnValue(mockDialogRef);
      
      component.editUser(mockUsers[0]);
      
      expect(userService.getMembers).toHaveBeenCalledTimes(1); // Only in ngOnInit
    });
  });

  describe('Loading States', () => {
    it('should show loading state during API call', fakeAsync(() => {
      userService.getMembers.and.returnValue(scheduled([mockUsers], asyncScheduler));

      component.loadUsers();

      // Right after calling loadUsers(), the loading flag should be true
      expect(component.loading()).toBeTrue();
    }));

    it('should hide loading after API response', fakeAsync(() => {
      userService.getMembers.and.returnValue(scheduled([mockUsers], asyncScheduler));
      
      component.loadUsers();

      // Flush the scheduled async emission
      tick(); 

      // After the "response", it should be false again
      expect(component.loading()).toBeFalse();
    }));
  });

  describe('Error Handling', () => {
    it('should show error snackbar on API failure', () => {
      const errorMessage = 'API Error';
      userService.getMembers.and.returnValue(throwError(() => new Error(errorMessage)));
      
      component.ngOnInit();
      
      expect(snackBar.open).toHaveBeenCalledWith(
        'Failed to load users. Please try again.',
        'Close',
        { duration: 3000 }
      );
    });

    it('should show error snackbar on search failure', () => {
      userService.getMembers.and.returnValue(of(mockUsers));
      userService.search.and.returnValue(throwError(() => new Error('Search failed')));
      component.ngOnInit();
      
      component.searchFirstName.set('John');
      component.searchUsers();
      
      expect(snackBar.open).toHaveBeenCalledWith(
        'Failed to search users. Please try again.',
        'Close',
        { duration: 3000 }
      );
    });
  });

  describe('Navigation', () => {
    it('should navigate to /admin/books on back button click', () => {
      component.goBackToBooks();
      
      expect(router.navigate).toHaveBeenCalledWith(['/admin/books']);
    });
  });

  describe('Empty State', () => {
    it('should show empty state when no users found', () => {
      userService.getMembers.and.returnValue(of([]));
      
      component.ngOnInit();
      
      expect(component.users()).toEqual([]);
    });
  });

  describe('Helper Methods', () => {
    it('should return correct role color for ADMIN', () => {
      const color = component.getRoleColor(UserRole.ADMIN);
      expect(color).toBe('primary');
    });

    it('should return correct role color for MEMBER', () => {
      const color = component.getRoleColor(UserRole.MEMBER);
      expect(color).toBe('accent');
    });

    it('should return correct role display name for ADMIN', () => {
      const displayName = component.getRoleDisplayName(UserRole.ADMIN);
      expect(displayName).toBe('Admin');
    });

    it('should return correct role display name for MEMBER', () => {
      const displayName = component.getRoleDisplayName(UserRole.MEMBER);
      expect(displayName).toBe('Member');
    });
  });

  describe('Clear Search', () => {
    it('should clear search fields and reload users', () => {
      userService.getMembers.and.returnValue(of(mockUsers));
      component.searchFirstName.set('John');
      component.searchLastName.set('Doe');
      
      component.clearSearch();
      
      expect(component.searchFirstName()).toBe('');
      expect(component.searchLastName()).toBe('');
      expect(userService.getMembers).toHaveBeenCalled();
    });
  });
});
