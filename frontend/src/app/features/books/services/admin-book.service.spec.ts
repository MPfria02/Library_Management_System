import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AdminBookService } from './admin-book.service';
import { BookCreationRequest, BookGenre, BookResponse, BookAdminResponse, BookSearchFilters, PageResponse } from '../models/book.model';

describe('AdminBookService', () => {
  let service: AdminBookService;
  let httpMock: HttpTestingController;

  const baseRequest: BookCreationRequest = {
    isbn: '9780134685991',
    title: 'Effective Java',
    author: 'Joshua Bloch',
    description: 'Best practices for the Java platform',
    genre: BookGenre.TECHNOLOGY,
    totalCopies: 5,
    availableCopies: 5,
    publicationDate: '2018-01-01'
  };

  const adminResponse: BookAdminResponse = {
    id: 1,
    isbn: baseRequest.isbn,
    title: baseRequest.title,
    author: baseRequest.author,
    description: baseRequest.description,
    genre: baseRequest.genre,
    totalCopies: baseRequest.totalCopies!,
    availableCopies: baseRequest.availableCopies!,
    publicationDate: baseRequest.publicationDate,
    createdAt: '2023-01-01T00:00:00Z',
    updatedAt: '2023-01-01T00:00:00Z',
  };
  const pageResponse: PageResponse<BookAdminResponse> = {
    content: [adminResponse],
    totalPages: 1,
    totalElements: 1,
    size: 30,
    number: 0,
    first: true,
    last: true,
    empty: false
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AdminBookService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('createBook calls POST /api/admin/books with correct body', () => {
    service.createBook(baseRequest).subscribe(res => {
      expect(res).toEqual(adminResponse);
    });

    const req = httpMock.expectOne('/api/admin/books');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(baseRequest);
    req.flush(adminResponse);
  });

  it('createBook returns created BookAdminResponse', () => {
    service.createBook(baseRequest).subscribe(res => {
      expect(res.id).toBe(1);
      expect(res.title).toBe('Effective Java');
      expect(res.isbn).toBe('9780134685991');
    });
    const req = httpMock.expectOne('/api/admin/books');
    req.flush(adminResponse);
  });

  it('updateBook calls PUT /api/admin/books/{id} with correct body', () => {
    const updatedRequest: BookCreationRequest = { ...baseRequest, title: 'Effective Java 3rd' };
    const updatedResponse: BookAdminResponse = { ...adminResponse, title: 'Effective Java 3rd' };

    service.updateBook(1, updatedRequest).subscribe(res => {
      expect(res).toEqual(updatedResponse);
    });

    const req = httpMock.expectOne('/api/admin/books/1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(updatedRequest);
    req.flush(updatedResponse);
  });

  it('updateBook returns updated BookAdminResponse', () => {
    const updatedResponse: BookAdminResponse = { ...adminResponse, title: 'Effective Java 3rd' };
    service.updateBook(1, { ...baseRequest, title: 'Effective Java 3rd' }).subscribe(res => {
      expect(res.title).toBe('Effective Java 3rd');
      expect(res.isbn).toBe('9780134685991');
    });
    const req = httpMock.expectOne('/api/admin/books/1');
    req.flush(updatedResponse);
  });

  it('deleteBook calls DELETE /api/admin/books/{id}', () => {
    service.deleteBook(1).subscribe(res => {
      expect(res).toBeUndefined();
    });
    const req = httpMock.expectOne('/api/admin/books/1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('deleteBook returns void observable', () => {
    service.deleteBook(2).subscribe(res => {
      expect(res).toBeUndefined();
    });
    const req = httpMock.expectOne('/api/admin/books/2');
    req.flush(null);
  });

  it('createBook handles 400 validation error', () => {
    const errorMsg = 'Validation failed';
    service.createBook(baseRequest).subscribe({
      next: () => fail('expected error'),
      error: (err: HttpErrorResponse) => {
        expect(err.status).toBe(400);
        expect(err.error).toBe(errorMsg);
      }
    });
    const req = httpMock.expectOne('/api/admin/books');
    req.flush(errorMsg, { status: 400, statusText: 'Bad Request' });
  });

  it('updateBook handles 404 not found', () => {
    service.updateBook(999, baseRequest).subscribe({
      next: () => fail('expected error'),
      error: (err: HttpErrorResponse) => {
        expect(err.status).toBe(404);
      }
    });
    const req = httpMock.expectOne('/api/admin/books/999');
    req.flush('Not Found', { status: 404, statusText: 'Not Found' });
  });

  it('deleteBook handles 403 forbidden (non-admin)', () => {
    service.deleteBook(1).subscribe({
      next: () => fail('expected error'),
      error: (err: HttpErrorResponse) => {
        expect(err.status).toBe(403);
      }
    });
    const req = httpMock.expectOne('/api/admin/books/1');
    req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });
  });

  it('getBooks calls GET /api/admin/books with correct pagination params', () => {
    service.getBooks(0, 30).subscribe(res => {
      expect(res).toEqual(pageResponse);
    });
    const req = httpMock.expectOne(r => r.method === 'GET' && r.url === '/api/admin/books');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('30');
    expect(req.request.params.get('sortBy')).toBe('title');
    expect(req.request.params.get('sortDir')).toBe('asc');
    req.flush(pageResponse);
  });

  it('getBooks includes filters in query string when provided', () => {
    const filters: BookSearchFilters = { searchTerm: 'Java', genre: BookGenre.TECHNOLOGY, availableOnly: true };
    service.getBooks(1, 15, filters).subscribe(res => {
      expect(res).toBeTruthy();
    });
    const req = httpMock.expectOne(r => r.method === 'GET' && r.url === '/api/admin/books');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('15');
    expect(req.request.params.get('searchTerm')).toBe('Java');
    expect(req.request.params.get('genre')).toBe('TECHNOLOGY');
    expect(req.request.params.get('availableOnly')).toBe('true');
    req.flush(pageResponse);
  });

  it('getBooks returns PageResponse with book data', () => {
    service.getBooks().subscribe(res => {
      expect(res.content[0].isbn).toBe(adminResponse.isbn);
      expect(res.content[0].title).toBe(adminResponse.title);
    });
    const req = httpMock.expectOne('/api/admin/books?page=0&size=30&sortBy=title&sortDir=asc');
    req.flush(pageResponse);
  });

  it('getBookById calls GET /api/admin/books/{id} with correct ID', () => {
    service.getBookById(42).subscribe(res => {
      expect(res.id).toBe(42);
      expect(res).toEqual({ ...adminResponse, id: 42 });
    });
    const req = httpMock.expectOne('/api/admin/books/42');
    expect(req.request.method).toBe('GET');
    req.flush({ ...adminResponse, id: 42 });
  });
});


