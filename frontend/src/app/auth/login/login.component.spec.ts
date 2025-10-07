import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
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
  
  beforeEach(async() => {
    mockAuthService = jasmine.createSpyObj('AuthService', ['login']);
    mockSnackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
  
    await TestBed.configureTestingModule({
      imports: [
        LoginComponent,
        ReactiveFormsModule,
        NoopAnimationsModule,
      ],
      providers: [
        provideRouter([]),
      ],
    })
    .overrideProvider(AuthService, { useValue: mockAuthService })
    .overrideProvider(MatSnackBar, { useValue: mockSnackBar })
    .compileComponents();

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

  it('should not submit when form is invalid', async() => {
    spyOn(component.loginForm, 'markAllAsTouched');
    await component.onSubmit();
    expect(component.loginForm.markAllAsTouched).toHaveBeenCalled();
    expect(mockAuthService.login).not.toHaveBeenCalled();
    expect(component.loading()).toBeFalse();
  });

  it('should show success snackbar on successful login', async () => {
    mockAuthService.login.and.returnValue(Promise.resolve());
    component.loginForm.setValue({
      email: 'test@example.com',
      password: '123456'
    });
    
    await component.onSubmit();  
    
    expect(mockSnackBar.open).toHaveBeenCalledWith(
      MSG_LOGIN_SUCCESS, 
      SNACKBAR_ACTION, 
      SNACKBAR_CONFIG
    );
    expect(component.loading()).toBeFalse();
  });

  it('should show error snackbar on failed login', async () => {
    const error = new Error(MSG_INVALID_EMAIL_OR_PASSWORD);
    mockAuthService.login.and.returnValue(Promise.reject(error));
    component.loginForm.setValue({
      email: 'test@example.com',
      password: '123456'
    });
    
    await component.onSubmit(); 
    
    expect(mockSnackBar.open).toHaveBeenCalledWith(
      MSG_INVALID_EMAIL_OR_PASSWORD, 
      SNACKBAR_ACTION, 
      SNACKBAR_CONFIG
    );
    expect(component.loading()).toBeFalse();
  });

  it('should manage button disabled state', () => {
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');

    // Initially invalid
    expect(button.disabled).toBeTrue();

    // Valid form
    component.loginForm.setValue({
      email: 'test@example.com',
      password: '123456'
    });
    fixture.detectChanges();
    expect(button.disabled).toBeFalse();

    // Loading state
    component.loading.set(true);
    fixture.detectChanges();
    expect(button.disabled).toBeTrue();

    component.loading.set(false);
    fixture.detectChanges();
    expect(button.disabled).toBeFalse();
  });
});
