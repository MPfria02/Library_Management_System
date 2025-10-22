import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BorrowRecordService } from './borrow-record.service';
import { BorrowStatus } from '../models/borrow-record.model';
import { createMockBorrowRecord, createMockPageResponse } from '../testing/borrow-record.test-helpers';
import { provideHttpClient } from '@angular/common/http';
import { of } from 'rxjs';

describe('BorrowRecordService', () => {
  let service: BorrowRecordService;
  let httpMock: HttpTestingController;
  const apiUrl = '/api/inventory/books';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(BorrowRecordService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Verify no outstanding HTTP requests
    httpMock.verify();
  });

  describe('getBorrowRecords', () => {
    it('should fetch borrow records with default pagination', (done) => {
      // Arrange
      const mockRecords = [createMockBorrowRecord()];
      const mockResponse = createMockPageResponse(mockRecords);

      // Act
      service.getBorrowRecords().subscribe({
        next: (response) => {
          // Assert
          expect(response).toEqual(mockResponse);
          expect(response.content.length).toBe(1);
          done();
        }
      });

      // Assert HTTP request
      const req = httpMock.expectOne((request) => {
        return request.url === apiUrl && 
               request.params.get('page') === '0' &&
               request.params.get('size') === '10';
      });
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should include custom page and size parameters', (done) => {
      // Arrange
      const page = 2;
      const size = 20;
      const mockResponse = createMockPageResponse([]);

      // Act
      service.getBorrowRecords(page, size).subscribe(() => done());

      // Assert
      const req = httpMock.expectOne((request) => {
        return request.params.get('page') === '2' &&
               request.params.get('size') === '20';
      });
      req.flush(mockResponse);
    });

    it('should include status parameter when filtering by BORROWED', (done) => {
      // Arrange
      const mockResponse = createMockPageResponse([]);

      // Act
      service.getBorrowRecords(0, 10, BorrowStatus.BORROWED).subscribe(() => done());

      // Assert
      const req = httpMock.expectOne((request) => {
        return request.params.get('status') === 'BORROWED';
      });
      req.flush(mockResponse);
    });

    it('should include status parameter when filtering by RETURNED', (done) => {
      // Arrange
      const mockResponse = createMockPageResponse([]);

      // Act
      service.getBorrowRecords(0, 10, BorrowStatus.RETURNED).subscribe(() => done());

      // Assert
      const req = httpMock.expectOne((request) => {
        return request.params.get('status') === 'RETURNED';
      });
      req.flush(mockResponse);
    });

    it('should NOT include status parameter when fetching all records', (done) => {
      // Arrange
      const mockResponse = createMockPageResponse([]);

      // Act
      service.getBorrowRecords(0, 10, null).subscribe(() => done());

      // Assert
      const req = httpMock.expectOne((request) => {
        return request.params.get('status') === null;
      });
      req.flush(mockResponse);
    });

    it('should handle empty results gracefully', (done) => {
      // Arrange
      const mockResponse = createMockPageResponse([]);

      // Act
      service.getBorrowRecords().subscribe({
        next: (response) => {
          // Assert
          expect(response.content.length).toBe(0);
          expect(response.empty).toBe(true);
          done();
        }
      });

      const req = httpMock.expectOne((request) => {
        return request.url === apiUrl && 
               request.params.get('page') === '0' &&
               request.params.get('size') === '10';
      });
      req.flush(mockResponse);
    });
  });

  describe('getActiveBorrows', () => {
    it('should call getBorrowRecords with BORROWED status', (done) => {
      // Arrange
      const mockResponse = createMockPageResponse([createMockBorrowRecord()]);
      spyOn(service, 'getBorrowRecords').and.returnValue(of(mockResponse));

      // Act
      service.getActiveBorrows().subscribe({
        next: () => {
          // Assert
          expect(service.getBorrowRecords).toHaveBeenCalledWith(
            0, 
            10, 
            BorrowStatus.BORROWED
          );
          done();
        }
      });
    });
  });

  describe('getBorrowHistory', () => {
    it('should call getBorrowRecords with RETURNED status', (done) => {
      // Arrange
      const mockResponse = createMockPageResponse([]);
      spyOn(service, 'getBorrowRecords').and.returnValue(of(mockResponse));

      // Act
      service.getBorrowHistory().subscribe({
        next: () => {
          // Assert
          expect(service.getBorrowRecords).toHaveBeenCalledWith(
            0, 
            10, 
            BorrowStatus.RETURNED
          );
          done();
        }
      });
    });
  });
});