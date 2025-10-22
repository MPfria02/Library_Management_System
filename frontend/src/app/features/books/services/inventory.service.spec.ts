import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { InventoryService } from './inventory.service';
import { BookGenre, BookResponse } from '../models/book.model';
import { BorrowStatus, BorrowStatusResponse } from '../../borrows/models/borrow-record.model';
import { createMockBorrowRecord } from '../../borrows/testing/borrow-record.test-helpers';

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

  describe('Service Initialization', () => {
    it('should be created', () => {
    expect(service).toBeTruthy();
  });
  })
  

  describe('borrowBook', () => {
    it('should return BorrowRecordResponse', (done) => {
      // Arrange
      const bookId = 1;
      const mockBorrowRecord = createMockBorrowRecord({ bookId });

      // Act
      service.borrowBook(bookId).subscribe({
        next: (response) => {
          // Assert - verify it's BorrowRecordResponse
          expect(response.id).toBeDefined();
          expect(response.bookId).toBe(bookId);
          expect(response.dueDate).toBeDefined();
          expect(response.borrowDate).toBeDefined();
          expect(response.status).toBe(BorrowStatus.BORROWED);
          done();
        }
      });

      const req = httpMock.expectOne(`/api/inventory/books/${bookId}/borrow`);
      expect(req.request.method).toBe('POST');
      req.flush(mockBorrowRecord);
    });
  });

  describe('returnBook (updated)', () => {
    it('should return BorrowRecordResponse with return date', (done) => {
      // Arrange
      const bookId = 1;
      const mockBorrowRecord = createMockBorrowRecord({
        bookId,
        status: BorrowStatus.RETURNED,
        returnDate: '2025-10-14'
      });

      // Act
      service.returnBook(bookId).subscribe({
        next: (response) => {
          // Assert
          expect(response.status).toBe(BorrowStatus.RETURNED);
          expect(response.returnDate).toBeDefined();
          expect(response.returnDate).not.toBeNull();
          done();
        }
      });

      const req = httpMock.expectOne(`/api/inventory/books/${bookId}/return`);
      expect(req.request.method).toBe('POST');
      req.flush(mockBorrowRecord);
    });
  });

  describe('checkBorrowStatus', () => {
    it('should return borrowed status as true when user has borrowed book', (done) => {
      // Arrange
      const bookId = 5;
      const mockResponse: BorrowStatusResponse = { borrowed: true };

      // Act
      service.checkBorrowStatus(bookId).subscribe({
        next: (response) => {
          // Assert
          expect(response.borrowed).toBe(true);
          done();
        }
      });

      const req = httpMock.expectOne(`/api/inventory/books/${bookId}/status`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should return borrowed status as false when user has not borrowed book', (done) => {
      // Arrange
      const bookId = 5;
      const mockResponse: BorrowStatusResponse = { borrowed: false };

      // Act
      service.checkBorrowStatus(bookId).subscribe({
        next: (response) => {
          // Assert
          expect(response.borrowed).toBe(false);
          done();
        }
      });

      const req = httpMock.expectOne(`/api/inventory/books/${bookId}/status`);
      req.flush(mockResponse);
    });
  });
});


