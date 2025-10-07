import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { AuthService, LoginCredentials, RegisterCredentials } from './auth.service';

function createMockJWT(payload: any): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const encodedPayload = btoa(JSON.stringify(payload));
  const signature = 'mock-signature';
  return `${header}.${encodedPayload}.${signature}`;
}

const TEST_TOKEN = 'test-jwt-token';
const MSG_INVALID_EMAIL_OR_PASSWORD = 'Invalid email or password';
const UNAUTHORIZED_STATUS = 401;
const MSG_UNAUTHORIZED = 'Unauthorized';
const MSG_USER_ALREADY_EXISTS = 'User already exists';
const CONFLICT_STATUS = 409;
const MSG_CONFLICT = 'Conflict';


const createLoginCredentials = (): LoginCredentials => {
  return {
    email: 'test@example.com',
    password: 'password'
  };
};

const createRegisterCredentials = (): RegisterCredentials => {
  return {
    email: 'test@example.com',
    password: 'password',
    firstName: 'testUser',
    lastName: 'testUserLastName',
    phone: '12345678'
  };
};


describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('login', () => {
    it('should POST to /api/auth/login and store token on success', async () => {
      const creds: LoginCredentials = createLoginCredentials();
      const promise = service.login(creds);

      const req = httpMock.expectOne('/api/auth/login');
      expect(req.request.method).toBe('POST');
      req.flush({ 
        token: TEST_TOKEN 
      });

      await promise;
      expect(localStorage.getItem('auth_token')).toBe(TEST_TOKEN);
    });

    it('should throw Error with backend message on 401', async () => {
      const creds: LoginCredentials = createLoginCredentials();
      const promise = service.login(creds);

      const req = httpMock.expectOne('/api/auth/login');
      expect(req.request.method).toBe('POST');
      req.flush({
          message: MSG_INVALID_EMAIL_OR_PASSWORD,
        },
        { 
          status: UNAUTHORIZED_STATUS,
          statusText: MSG_UNAUTHORIZED 
        }
      );

      await expectAsync(promise).toBeRejectedWithError(MSG_INVALID_EMAIL_OR_PASSWORD);
    });

    it('should throw generic Error on network error', async () => {
      const creds: LoginCredentials = createLoginCredentials();
      const promise = service.login(creds);

      const req = httpMock.expectOne('/api/auth/login');
      req.error(new ProgressEvent('NetworkError'));

      await expectAsync(promise).toBeRejected();
    });
  });

  describe('register', () => {
    it('should POST to /api/auth/register when credentials are valid', async () => {
      const creds: RegisterCredentials = createRegisterCredentials();
      const response = service.register(creds);

      const req = httpMock.expectOne('/api/auth/register');
      expect(req.request.method).toBe('POST');

      req.flush({});

      await expectAsync(response).toBeResolved();
    });

    it('should throw Error with backend message on 409', async () => {
      const creds: RegisterCredentials = createRegisterCredentials();
      const promise = service.register(creds);

      const req = httpMock.expectOne('/api/auth/register');
      expect(req.request.method).toBe('POST');
      req.flush(
        { 
          message: MSG_USER_ALREADY_EXISTS 
        },
        { 
          status: CONFLICT_STATUS,
          statusText: MSG_CONFLICT 
        }
      );

      await expectAsync(promise).toBeRejectedWithError(MSG_USER_ALREADY_EXISTS);
    });
  });

  describe('isLoggedIn', () => {
    it('should return false when no token', () => {
      expect(service.isLoggedIn()).toBeFalse();
    });

    it('should return true for valid (unexpired) token', () => {
      const token = createMockJWT({ sub: 'user@example.com', exp: Math.floor(Date.now() / 1000) + 3600 });
      localStorage.setItem('auth_token', token);
      expect(service.isLoggedIn()).toBeTrue();
    });

    it('should return false for expired token', () => {
      const token = createMockJWT({ sub: 'user@example.com', exp: Math.floor(Date.now() / 1000) - 10 });
      localStorage.setItem('auth_token', token);
      expect(service.isLoggedIn()).toBeFalse();
    });

    it('should return false for malformed token', () => {
      localStorage.setItem('auth_token', 'invalid.token');
      expect(service.isLoggedIn()).toBeFalse();
    });

    it('should return false when exp claim missing', () => {
      const token = createMockJWT({ sub: 'user@example.com' });
      localStorage.setItem('auth_token', token);
      expect(service.isLoggedIn()).toBeFalse();
    });
  });

  describe('getUserRole', () => {
    it('should return ADMIN role', () => {
      const token = createMockJWT({ role: 'ADMIN', exp: Math.floor(Date.now() / 1000) + 3600 });
      localStorage.setItem('auth_token', token);
      expect(service.getUserRole()).toBe('ADMIN');
    });

    it('should return MEMBER role', () => {
      const token = createMockJWT({ role: 'MEMBER', exp: Math.floor(Date.now() / 1000) + 3600 });
      localStorage.setItem('auth_token', token);
      expect(service.getUserRole()).toBe('MEMBER');
    });

    it('should return null when no token', () => {
      expect(service.getUserRole()).toBeNull();
    });

    it('should return null when role missing', () => {
      const token = createMockJWT({ sub: 'user@example.com', exp: Math.floor(Date.now() / 1000) + 3600 });
      localStorage.setItem('auth_token', token);
      expect(service.getUserRole()).toBeNull();
    });

    it('should return null for malformed token', () => {
      localStorage.setItem('auth_token', 'bad.token');
      expect(service.getUserRole()).toBeNull();
    });
  });

  describe('logout', () => {
    it('should remove token when present', () => {
      localStorage.setItem('auth_token', 'token');
      service.logout();
      expect(localStorage.getItem('auth_token')).toBeNull();
    });

    it('should not throw if token missing', () => {
      expect(() => service.logout()).not.toThrow();
    });
  });
});
