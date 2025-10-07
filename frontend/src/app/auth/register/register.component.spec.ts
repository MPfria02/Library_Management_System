import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { RegisterComponent } from './register.component';
import { AuthService } from '../auth.service';

const MSG_REGISTRATION_SUCCESS = 'Registration successful!';
const MSG_USER_ALREADY_EXISTS = 'User already exists';
const SNACKBAR_ACTION = 'Close';
const SNACKBAR_CONFIG = { duration: 3000 } as const;

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let mockAuthService: jasmine.SpyObj<AuthService>;
  let mockSnackBar: jasmine.SpyObj<MatSnackBar>;
  let mockRouter: jasmine.SpyObj<Router>;

  beforeEach(async() => {
    mockAuthService = jasmine.createSpyObj('AuthService', ['register']);
    mockSnackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [
        RegisterComponent,
        ReactiveFormsModule,
        NoopAnimationsModule,
      ],
    })
    .overrideProvider(AuthService, { useValue: mockAuthService })
    .overrideProvider(MatSnackBar, { useValue: mockSnackBar })
    .overrideProvider(Router, { useValue: mockRouter })
    .compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create form with required controls and be invalid initially', () => {
    expect(component.registerForm.contains('email')).toBeTrue();
    expect(component.registerForm.contains('password')).toBeTrue();
    expect(component.registerForm.contains('firstName')).toBeTrue();
    expect(component.registerForm.contains('lastName')).toBeTrue();
    expect(component.registerForm.contains('phone')).toBeTrue();
    expect(component.registerForm.valid).toBeFalse();
  });

  it('should validate form controls', () => {
    const { email, password, firstName, lastName, phone } = component.registerForm.controls as any;

    email.setValue('');
    password.setValue('');
    firstName.setValue('');
    lastName.setValue('');
    phone.setValue('');

    expect(email.hasError('required')).toBeTrue();
    expect(password.hasError('required')).toBeTrue();
    expect(firstName.hasError('required')).toBeTrue();
    expect(lastName.hasError('required')).toBeTrue();
    expect(phone.hasError('required')).toBeTrue();

    email.setValue('invalid');
    expect(email.hasError('email')).toBeTrue();

    password.setValue('123');
    expect(password.hasError('minlength')).toBeTrue();

    firstName.setValue('A'.repeat(51));
    expect(firstName.hasError('maxlength')).toBeTrue();

    lastName.setValue('A'.repeat(51));
    expect(lastName.hasError('maxlength')).toBeTrue();

    phone.setValue('1'.repeat(21));
    expect(phone.hasError('maxlength')).toBeTrue();

    component.registerForm.setValue({
      email: 'test@example.com',
      password: '123456',
      firstName: 'Alice',
      lastName: 'Smith',
      phone: '123467890',
    });
    expect(component.registerForm.valid).toBeTrue();
  });

  it('should not submit when form is invalid', () => {
    spyOn(component.registerForm, 'markAllAsTouched');
    component.onSubmit();
    expect(component.registerForm.markAllAsTouched).toHaveBeenCalled();
    expect(mockAuthService.register).not.toHaveBeenCalled();
    expect(component.loading()).toBeFalse();
  });

  it('should show success snackbar on successful registration', async() => {
    mockAuthService.register.and.returnValue(Promise.resolve());
       component.registerForm.setValue({
      email: 'test@example.com',
      password: '123456',
      firstName: 'Alice',
      lastName: 'Smith',
      phone: '123467890',
    });

    await component.onSubmit();
    
    expect(mockSnackBar.open).toHaveBeenCalledWith(
      MSG_REGISTRATION_SUCCESS,
      SNACKBAR_ACTION,
      SNACKBAR_CONFIG
    );
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/login']);
    expect(component.loading()).toBeFalse();
 
  });

  it('should show error snackbar on failed registration', async() => {
    const error = new Error(MSG_USER_ALREADY_EXISTS);
    mockAuthService.register.and.returnValue(Promise.reject(error));

       component.registerForm.setValue({
      email: 'test@example.com',
      password: '123456',
      firstName: 'Alice',
      lastName: 'Smith',
      phone: '123467890',
    });

    await component.onSubmit();

    expect(mockSnackBar.open).toHaveBeenCalledWith(
      MSG_USER_ALREADY_EXISTS,
      SNACKBAR_ACTION,
      SNACKBAR_CONFIG
    );
    expect(mockRouter.navigate).not.toHaveBeenCalled();
    expect(component.loading()).toBeFalse();
    
  });

  it('should manage button disabled state', () => {
    const button: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');

    expect(button.disabled).toBeTrue();

       component.registerForm.setValue({
      email: 'test@example.com',
      password: '123456',
      firstName: 'Alice',
      lastName: 'Smith',
      phone: '123467890',
    });
    fixture.detectChanges();
    expect(button.disabled).toBeFalse();

    component.loading.set(true);
    fixture.detectChanges();
    expect(button.disabled).toBeTrue();

    component.loading.set(false);
    fixture.detectChanges();
    expect(button.disabled).toBeFalse();
  });
});
