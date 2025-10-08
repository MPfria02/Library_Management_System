import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { InventoryService } from './inventory.service';
import { BookGenre, BookResponse } from '../models/book.model';

describe('InventoryService', () => {
  let service: InventoryService;
  let httpMock: HttpTestingController;

  const baseBook: BookResponse = {
    id: 1,
    title: 'Effective Java',
    author: 'Joshua Bloch',
    description: 'Best practices for the Java platform',
    genre: BookGenre.TECHNOLOGY,
    availableCopies: 3,
    publicationDate: '2018-01-01'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(InventoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call POST /api/inventory/books/{id}/borrow', () => {
    const bookId = 1;
    const afterBorrow: BookResponse = { ...baseBook, availableCopies: 2 };

    service.borrowBook(bookId).subscribe(response => {
      expect(response).toBeTruthy();
      expect(response).toEqual(afterBorrow);
    });

    const req = httpMock.expectOne(`/api/inventory/books/${bookId}/borrow`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush(afterBorrow);
  });

  it('should return updated BookResponse after borrow (availableCopies decreased)', () => {
    const bookId = 1;
    const afterBorrow: BookResponse = { ...baseBook, availableCopies: 2 };

    service.borrowBook(bookId).subscribe(response => {
      expect(response.availableCopies).toBe(2);
    });

    const req = httpMock.expectOne(`/api/inventory/books/${bookId}/borrow`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush(afterBorrow);
  });

  it('should call POST /api/inventory/books/{id}/return', () => {
    const bookId = 1;
    const beforeReturn: BookResponse = { ...baseBook, availableCopies: 1 };
    const afterReturn: BookResponse = { ...beforeReturn, availableCopies: 2 };

    service.returnBook(bookId).subscribe(response => {
      expect(response).toEqual(afterReturn);
    });

    const req = httpMock.expectOne(`/api/inventory/books/${bookId}/return`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush(afterReturn);
  });

  it('should return updated BookResponse after return (availableCopies increased)', () => {
    const bookId = 1;
    const beforeReturn: BookResponse = { ...baseBook, availableCopies: 1 };
    const afterReturn: BookResponse = { ...beforeReturn, availableCopies: 2 };

    service.returnBook(bookId).subscribe(response => {
      expect(response.availableCopies).toBe(2);
    });

    const req = httpMock.expectOne(`/api/inventory/books/${bookId}/return`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeNull();
    req.flush(afterReturn);
  });

  it('should handle 400 error when book unavailable', () => {
    const bookId = 1;
    const errorMessage = 'No copies available';

    service.borrowBook(bookId).subscribe({
      next: () => fail('Should have failed'),
      error: error => {
        expect(error.status).toBe(400);
        expect(error.error).toContain('No copies available');
      }
    });

    const req = httpMock.expectOne(`/api/inventory/books/${bookId}/borrow`);
    expect(req.request.method).toBe('POST');
    req.flush(errorMessage, { status: 400, statusText: 'Bad Request' });
  });

  it('should handle 404 and 500 errors gracefully', () => {
    const bookId = 999;

    // 404 Not Found
    service.borrowBook(bookId).subscribe({
      next: () => fail('Should have failed with 404'),
      error: error => {
        expect(error.status).toBe(404);
      }
    });
    let req = httpMock.expectOne(`/api/inventory/books/${bookId}/borrow`);
    req.flush('Not Found', { status: 404, statusText: 'Not Found' });

    // 500 Server Error
    service.returnBook(bookId).subscribe({
      next: () => fail('Should have failed with 500'),
      error: error => {
        expect(error.status).toBe(500);
      }
    });
    req = httpMock.expectOne(`/api/inventory/books/${bookId}/return`);
    req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
  });
});


