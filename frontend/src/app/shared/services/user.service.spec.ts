import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { UserService } from './user.service';
import { UserResponse, UserRole } from '../models/user.model';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  const mockUser: UserResponse = {
    id: 1,
    email: 'john.doe@example.com',
    firstName: 'John',
    lastName: 'Doe',
    phone: '+1234567890',
    role: UserRole.MEMBER
  };

  const mockAdminUser: UserResponse = {
    id: 2,
    email: 'admin@example.com',
    firstName: 'Admin',
    lastName: 'User',
    phone: '+1234567891',
    role: UserRole.ADMIN
  };

  const mockUsers: UserResponse[] = [mockUser, mockAdminUser];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getById', () => {
    it('calls GET /api/users/{id} with correct ID', () => {
      service.getById(1).subscribe();
      
      const req = httpMock.expectOne('/api/users/1');
      expect(req.request.method).toBe('GET');
      req.flush(mockUser);
    });

    it('returns UserResponse', () => {
      service.getById(1).subscribe(res => {
        expect(res).toEqual(mockUser);
        expect(res.id).toBe(1);
        expect(res.email).toBe('john.doe@example.com');
      });
      
      const req = httpMock.expectOne('/api/users/1');
      req.flush(mockUser);
    });
  });

  describe('getMembers', () => {
    it('calls GET /api/users', () => {
      service.getMembers().subscribe();
      
      const req = httpMock.expectOne('/api/users');
      expect(req.request.method).toBe('GET');
      req.flush(mockUsers);
    });

    it('returns array of UserResponse', () => {
      service.getMembers().subscribe(res => {
        expect(res).toEqual(mockUsers);
        expect(res.length).toBe(2);
        expect(res[0].firstName).toBe('John');
      });
      
      const req = httpMock.expectOne('/api/users');
      req.flush(mockUsers);
    });
  });

  describe('search', () => {
    it('calls GET /api/users/search with both params', () => {
      service.search('John', 'Doe').subscribe();
      
      const req = httpMock.expectOne('/api/users/search?firstName=John&lastName=Doe');
      expect(req.request.method).toBe('GET');
      req.flush([mockUser]);
    });

    it('includes only provided params in query string', () => {
      service.search('John', undefined).subscribe();
      
      const req = httpMock.expectOne('/api/users/search?firstName=John');
      expect(req.request.method).toBe('GET');
      req.flush([mockUser]);
    });

    it('handles null/undefined params correctly', () => {
      service.search(undefined, 'Doe').subscribe();
      
      const req = httpMock.expectOne('/api/users/search?lastName=Doe');
      expect(req.request.method).toBe('GET');
      req.flush([mockUser]);
    });

    it('handles both params as null/undefined', () => {
      service.search(undefined, undefined).subscribe();
      
      const req = httpMock.expectOne('/api/users/search');
      expect(req.request.method).toBe('GET');
      req.flush(mockUsers);
    });
  });

  describe('findByRole', () => {
    it('calls GET /api/users/role/{role} with correct role', () => {
      service.findByRole(UserRole.MEMBER).subscribe();
      
      const req = httpMock.expectOne('/api/users/role/MEMBER');
      expect(req.request.method).toBe('GET');
      req.flush([mockUser]);
    });

    it('returns filtered user list', () => {
      service.findByRole(UserRole.ADMIN).subscribe(res => {
        expect(res).toEqual([mockAdminUser]);
        expect(res.length).toBe(1);
        expect(res[0].role).toBe(UserRole.ADMIN);
      });
      
      const req = httpMock.expectOne('/api/users/role/ADMIN');
      req.flush([mockAdminUser]);
    });
  });

  describe('update', () => {
    it('calls PUT /api/users/{id} with correct body', () => {
      const updatedUser: UserResponse = { ...mockUser, firstName: 'Jane' };
      
      service.update(1, updatedUser).subscribe();
      
      const req = httpMock.expectOne('/api/users/1');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(updatedUser);
      req.flush(updatedUser);
    });

    it('returns updated UserResponse', () => {
      const updatedUser: UserResponse = { ...mockUser, firstName: 'Jane' };
      
      service.update(1, updatedUser).subscribe(res => {
        expect(res).toEqual(updatedUser);
        expect(res.firstName).toBe('Jane');
        expect(res.id).toBe(1);
      });
      
      const req = httpMock.expectOne('/api/users/1');
      req.flush(updatedUser);
    });
  });

  describe('countAll', () => {
    it('calls GET /api/users/count and returns number', () => {
      service.countAll().subscribe(res => {
        expect(res).toBe(42);
        expect(typeof res).toBe('number');
      });
      
      const req = httpMock.expectOne('/api/users/count');
      expect(req.request.method).toBe('GET');
      req.flush(42);
    });
  });
});
