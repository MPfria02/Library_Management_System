import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { adminGuard } from './admin.guard';
import { AuthService } from '../auth.service';

const MSG_LOGIN_REQUIRED = 'Please login to access this page';
const MSG_ACCESS_DENIED = 'Access denied. Admin privileges required';
const SNACKBAR_ACTION = 'Close';
const SNACKBAR_CONFIG = { duration: 3000 } as const;
const URL_TO_NAVIGATE = '/dashboard';

describe('adminGuard', () => {
  let mockAuthService: jasmine.SpyObj<AuthService>;
  let mockRouter: jasmine.SpyObj<Router>;
  let mockSnackBar: jasmine.SpyObj<MatSnackBar>;

  beforeEach(() => {
    mockAuthService = jasmine.createSpyObj('AuthService', ['isLoggedIn', 'getUserRole']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);
    mockSnackBar = jasmine.createSpyObj('MatSnackBar', ['open']);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        { provide: Router, useValue: mockRouter },
        { provide: MatSnackBar, useValue: mockSnackBar },
      ],
    });
  });

  it('should return false and redirect to login when user is not logged in', () => {
    mockAuthService.isLoggedIn.and.returnValue(false);

    const result = TestBed.runInInjectionContext(() => adminGuard({} as any, {} as any));

    expect(result).toBeFalse();
    expect(mockAuthService.isLoggedIn).toHaveBeenCalled();
    expect(mockAuthService.getUserRole).not.toHaveBeenCalled();
    expect(mockSnackBar.open).toHaveBeenCalledWith(
      MSG_LOGIN_REQUIRED,
      SNACKBAR_ACTION,
      SNACKBAR_CONFIG
    );
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should return true when user is logged in as ADMIN', () => {
    mockAuthService.isLoggedIn.and.returnValue(true);
    mockAuthService.getUserRole.and.returnValue('ADMIN');

    const result = TestBed.runInInjectionContext(() => adminGuard({} as any, {} as any));

    expect(result).toBeTrue();
    expect(mockAuthService.isLoggedIn).toHaveBeenCalled();
    expect(mockAuthService.getUserRole).toHaveBeenCalled();
    expect(mockSnackBar.open).not.toHaveBeenCalled();
    expect(mockRouter.navigate).not.toHaveBeenCalled();
  });

  it('should return false and redirect to dashboard when user is MEMBER', () => {
    mockAuthService.isLoggedIn.and.returnValue(true);
    mockAuthService.getUserRole.and.returnValue('MEMBER');

    const result = TestBed.runInInjectionContext(() => adminGuard({} as any, {} as any));

    expect(result).toBeFalse();
    expect(mockSnackBar.open).toHaveBeenCalledWith(
      MSG_ACCESS_DENIED,
      SNACKBAR_ACTION,
      SNACKBAR_CONFIG
    );
    expect(mockRouter.navigate).toHaveBeenCalledWith([URL_TO_NAVIGATE]);
  });

  it('should return false and redirect to dashboard when role is null', () => {
    mockAuthService.isLoggedIn.and.returnValue(true);
    mockAuthService.getUserRole.and.returnValue(null);

    const result = TestBed.runInInjectionContext(() => adminGuard({} as any, {} as any));

    expect(result).toBeFalse();
    expect(mockSnackBar.open).toHaveBeenCalledWith(
      MSG_ACCESS_DENIED,
      SNACKBAR_ACTION,
      SNACKBAR_CONFIG
    );
    expect(mockRouter.navigate).toHaveBeenCalledWith([URL_TO_NAVIGATE]);
  });

  it('should return false and redirect to dashboard when role is undefined', () => {
    mockAuthService.isLoggedIn.and.returnValue(true);
    mockAuthService.getUserRole.and.returnValue(undefined as unknown as string);

    const result = TestBed.runInInjectionContext(() => adminGuard({} as any, {} as any));

    expect(result).toBeFalse();
    expect(mockSnackBar.open).toHaveBeenCalledWith(
      MSG_ACCESS_DENIED,
      SNACKBAR_ACTION,
      SNACKBAR_CONFIG
    );
    expect(mockRouter.navigate).toHaveBeenCalledWith([URL_TO_NAVIGATE]);
  });
});
