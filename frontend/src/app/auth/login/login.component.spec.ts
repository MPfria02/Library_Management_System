import { ComponentFixture, fakeAsync, flushMicrotasks, TestBed, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router, Routes } from '@angular/router';
import { LoginComponent } from './login.component';
import { AuthService } from '../auth.service';

const MSG_LOGIN_SUCCESS = 'Login successful!';
const MSG_INVALID_EMAIL_OR_PASSWORD = 'Invalid email or password';
const SNACKBAR_ACTION = 'Close';
const SNACKBAR_CONFIG = { duration: 3000 } as const;

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let mockAuthService: jasmine.SpyObj<AuthService>;
  let mockSnackBar: jasmine.SpyObj<MatSnackBar>;
  let mockRouter: Router;

  beforeEach(async () => {
    mockAuthService = jasmine.createSpyObj('AuthService', ['login']);
    mockSnackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent, ReactiveFormsModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: mockAuthService},
      ],
    })
    .overrideProvider(MatSnackBar, { useValue: mockSnackBar })
    .compileComponents();

    mockRouter = TestBed.inject(Router);
    spyOn(mockRouter, 'navigate');

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create form with email and password controls and be invalid initially', () => {
    expect(component.loginForm.contains('email')).toBeTrue();
    expect(component.loginForm.contains('password')).toBeTrue();
    expect(component.loginForm.valid).toBeFalse();
  });

  it('should validate form controls', () => {
    const email = component.email!;
    const password = component.password!;

    email.setValue('');
    password.setValue('');
    expect(email.hasError('required')).toBeTrue();
    expect(password.hasError('required')).toBeTrue();

    email.setValue('invalid');
    expect(email.hasError('email')).toBeTrue();

    password.setValue('123');
    expect(password.hasError('minlength')).toBeTrue();

    email.setValue('test@example.com');
    password.setValue('123456');
    expect(component.loginForm.valid).toBeTrue();
  });

  it('should not submit when form is invalid', fakeAsync(() => {
    spyOn(component.loginForm, 'markAllAsTouched');
    
    // Form is invalid (no values set)
    component.onSubmit();
    tick();
    
    expect(component.loginForm.markAllAsTouched).toHaveBeenCalled();
    expect(mockAuthService.login).not.toHaveBeenCalled();
    expect(component.loading()).toBeFalse();
  }));

  it('should show success snackbar and navigate to dashboard on successful login', fakeAsync(() => {
    // Setup successful login
    mockAuthService.login.and.returnValue(Promise.resolve());
    // Explicit role mock for MEMBER
    mockAuthService.getUserRole = jasmine.createSpy('getUserRole').and.returnValue('MEMBER');

    // Set valid form values
    component.loginForm.setValue({
      email: 'test@example.com',
      password: '123456',
    });

    // Submit form
    component.onSubmit();

    // Initially, loading should be true
    expect(component.loading()).toBeTrue();

    // Wait for all async operations to complete
    flushMicrotasks();

    // Verify all expected behaviors
    expect(mockAuthService.login).toHaveBeenCalledWith({
      email: 'test@example.com',
      password: '123456',
    });
    expect(mockAuthService.getUserRole).toHaveBeenCalled();
    expect(mockSnackBar.open).toHaveBeenCalledWith(
      MSG_LOGIN_SUCCESS,
      SNACKBAR_ACTION,
      SNACKBAR_CONFIG
    );
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/dashboard']);
    expect(component.loading()).toBeFalse();
  }));

  it('should navigate to /admin/books when login successful with ADMIN role', fakeAsync(() => {
    // Setup successful login
    mockAuthService.login.and.returnValue(Promise.resolve());
    mockAuthService.getUserRole = jasmine.createSpy('getUserRole').and.returnValue('ADMIN');

    // Set valid form values
    component.loginForm.setValue({
      email: 'admin@example.com',
      password: 'adminpass',
    });

    // Submit form
    component.onSubmit();
    expect(component.loading()).toBeTrue();
    flushMicrotasks();

    expect(mockAuthService.login).toHaveBeenCalledWith({
      email: 'admin@example.com',
      password: 'adminpass',
    });
    expect(mockAuthService.getUserRole).toHaveBeenCalled();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/admin/books']);
    expect(mockSnackBar.open).toHaveBeenCalledWith(
      MSG_LOGIN_SUCCESS,
      SNACKBAR_ACTION,
      SNACKBAR_CONFIG
    );
    expect(component.loading()).toBeFalse();
  }));

  it('should navigate to /dashboard when login successful with MEMBER role', fakeAsync(() => {
    // Setup successful login
    mockAuthService.login.and.returnValue(Promise.resolve());
    mockAuthService.getUserRole = jasmine.createSpy('getUserRole').and.returnValue('MEMBER');

    // Set valid form values
    component.loginForm.setValue({
      email: 'member@example.com',
      password: 'memberpass',
    });

    // Submit form
    component.onSubmit();
    expect(component.loading()).toBeTrue();
    flushMicrotasks();

    expect(mockAuthService.login).toHaveBeenCalledWith({
      email: 'member@example.com',
      password: 'memberpass',
    });
    expect(mockAuthService.getUserRole).toHaveBeenCalled();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/dashboard']);
    expect(mockSnackBar.open).toHaveBeenCalledWith(
      MSG_LOGIN_SUCCESS,
      SNACKBAR_ACTION,
      SNACKBAR_CONFIG
    );
    expect(component.loading()).toBeFalse();
  }));

  it('should show error snackbar on failed login', fakeAsync(() => {
    const error = new Error(MSG_INVALID_EMAIL_OR_PASSWORD);
    mockAuthService.login.and.returnValue(Promise.reject(error));
    
    component.loginForm.setValue({
      email: 'test@example.com',
      password: '123456',
    });

    component.onSubmit();
    
    // Initially loading should be true
    expect(component.loading()).toBeTrue();
    
    flushMicrotasks();

    expect(mockAuthService.login).toHaveBeenCalledWith({
      email: 'test@example.com',
      password: '123456',
    });
    expect(mockSnackBar.open).toHaveBeenCalledWith(
      MSG_INVALID_EMAIL_OR_PASSWORD,
      SNACKBAR_ACTION,
      SNACKBAR_CONFIG
    );
    expect(mockRouter.navigate).not.toHaveBeenCalled();
    expect(component.loading()).toBeFalse();
  }));

  it('should manage button disabled state', fakeAsync(() => {
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');

    // Initially invalid (form has no values)
    fixture.detectChanges();
    expect(button.disabled).toBeTrue();

    // Valid form
    component.loginForm.setValue({
      email: 'test@example.com',
      password: '123456',
    });
    fixture.detectChanges();
    expect(button.disabled).toBeFalse();

    // Loading state
    mockAuthService.login.and.returnValue(new Promise(() => {})); // Never resolves
    component.onSubmit();
    fixture.detectChanges();
    expect(button.disabled).toBeTrue();
    expect(component.loading()).toBeTrue();
  }));
});
