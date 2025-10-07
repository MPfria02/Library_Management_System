import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { authGuard } from './auth.guard';
import { AuthService } from '../auth.service';

const MSG_LOGIN_REQUIRED = 'Please login to access this page';
const SNACKBAR_ACTION = 'Close';
const SNACKBAR_CONFIG = { duration: 3000 } as const;

describe('authGuard', () => {
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

  it('should return true when user is logged in', () => {
    mockAuthService.isLoggedIn.and.returnValue(true);

    const result = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));

    expect(result).toBeTrue();
    expect(mockRouter.navigate).not.toHaveBeenCalled();
    expect(mockSnackBar.open).not.toHaveBeenCalled();
  });

  it('should return false when user is not logged in', () => {
    mockAuthService.isLoggedIn.and.returnValue(false);

    const result = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));

    expect(result).toBeFalse();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/login']);
    expect(mockSnackBar.open).toHaveBeenCalledWith(
      MSG_LOGIN_REQUIRED,
      SNACKBAR_ACTION,
      SNACKBAR_CONFIG
    );
  });

  it('should not call router or snackbar when logged in', () => {
    mockAuthService.isLoggedIn.and.returnValue(true);

    void TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));

    expect(mockAuthService.isLoggedIn).toHaveBeenCalledTimes(1);
    expect(mockRouter.navigate).not.toHaveBeenCalled();
    expect(mockSnackBar.open).not.toHaveBeenCalled();
  });
});
