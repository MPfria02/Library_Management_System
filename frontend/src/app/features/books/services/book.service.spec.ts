import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BookService } from './book.service';
import { BookGenre, BookResponse, BookSearchFilters, PageResponse } from '../models/book.model';
import { provideHttpClient } from '@angular/common/http';

describe('BookService', () => {
  let service: BookService;
  let httpMock: HttpTestingController;

  const mockBook: BookResponse = {
    id: 1,
    title: 'Effective Java',
    author: 'Joshua Bloch',
    description: 'Best practices for the Java platform',
    genre: BookGenre.TECHNOLOGY,
    availableCopies: 5,
    publicationDate: '2018-01-01'
  };

  const mockEmptyPage: PageResponse<BookResponse> = {
    content: [],
    totalPages: 0,
    totalElements: 0,
    size: 12,
    number: 0,
    first: true,
    last: true,
    empty: true
  };

  const buildPage = (items: BookResponse[]): PageResponse<BookResponse> => ({
    content: items,
    totalPages: 1,
    totalElements: items.length,
    size: items.length,
    number: 0,
    first: true,
    last: true,
    empty: items.length === 0
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
          ],
        });
    service = TestBed.inject(BookService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch paginated books with default params', () => {
    const mockResponse = buildPage([mockBook]);

    service.getBooks().subscribe(res => {
      expect(res).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(request => request.url === '/api/books');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('12');
    expect(req.request.params.get('sortBy')).toBe('title');
    expect(req.request.params.get('sortDir')).toBe('asc');
    req.flush(mockResponse);
  });

  it('should include page and size in request', () => {
    const mockResponse = buildPage([mockBook]);

    service.getBooks(2, 20).subscribe(res => {
      expect(res).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(request => request.url === '/api/books');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('20');
    req.flush(mockResponse);
  });

  it('should include sortBy and sortDir in request', () => {
    const mockResponse = buildPage([mockBook]);

    service.getBooks(0, 12).subscribe(res => {
      expect(res).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(request => request.url === '/api/books');
    expect(req.request.params.get('sortBy')).toBe('title');
    expect(req.request.params.get('sortDir')).toBe('asc');
    req.flush(mockResponse);
  });

  it('should fetch single book by ID', () => {
    service.getBookById(1).subscribe(res => {
      expect(res).toEqual(mockBook);
    });

    const req = httpMock.expectOne(request => request.url === '/api/books/1');
    expect(req.request.method).toBe('GET');
    req.flush(mockBook);
  });

  it('should fetch available genres', () => {
    const mockGenres: BookGenre[] = [
      BookGenre.TECHNOLOGY,
      BookGenre.SCIENCE,
      BookGenre.FICTION
    ];

    service.getAvailableGenres().subscribe(res => {
      expect(res).toEqual(mockGenres);
    });

    const req = httpMock.expectOne(request => request.url === '/api/books/genres');
    expect(req.request.method).toBe('GET');
    req.flush(mockGenres);
  });

  it('should add searchTerm to params when provided', () => {
    const mockResponse = buildPage([mockBook]);
    const filters: BookSearchFilters = { searchTerm: 'Java' };

    service.getBooks(0, 12, filters).subscribe(res => {
      expect(res).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(request => request.url === '/api/books' && request.params.has('searchTerm'));
    expect(req.request.params.get('searchTerm')).toBe('Java');
    req.flush(mockResponse);
  });

  it('should add genre to params when provided', () => {
    const mockResponse = buildPage([mockBook]);
    const filters: BookSearchFilters = { genre: BookGenre.TECHNOLOGY };

    service.getBooks(0, 12, filters).subscribe(res => {
      expect(res).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(request => request.url === '/api/books' && request.params.has('genre'));
    expect(req.request.params.get('genre')).toBe(BookGenre.TECHNOLOGY);
    req.flush(mockResponse);
  });

  it('should add availableOnly=true when filter is true', () => {
    const mockResponse = buildPage([mockBook]);
    const filters: BookSearchFilters = { availableOnly: true };

    service.getBooks(0, 12, filters).subscribe(res => {
      expect(res).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(request => request.url === '/api/books' && request.params.has('availableOnly'));
    expect(req.request.params.get('availableOnly')).toBe('true');
    req.flush(mockResponse);
  });

  it('should NOT add availableOnly when filter is false', () => {
    const mockResponse = buildPage([mockBook]);
    const filters: BookSearchFilters = { availableOnly: false };

    service.getBooks(0, 12, filters).subscribe(res => {
      expect(res).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(request => request.url === '/api/books');
    expect(req.request.params.has('availableOnly')).toBeFalse();
    req.flush(mockResponse);
  });

  it('should handle multiple filters simultaneously', () => {
    const mockResponse = buildPage([mockBook]);
    const filters: BookSearchFilters = {
      searchTerm: 'Effective',
      genre: BookGenre.TECHNOLOGY,
      availableOnly: true
    };

    service.getBooks(1, 10, filters).subscribe(res => {
      expect(res).toEqual(mockResponse);
    });

    const req = httpMock.expectOne(request => request.url === '/api/books');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('10');
    expect(req.request.params.get('searchTerm')).toBe('Effective');
    expect(req.request.params.get('genre')).toBe(BookGenre.TECHNOLOGY);
    expect(req.request.params.get('availableOnly')).toBe('true');
    req.flush(mockResponse);
  });

  it('should handle empty results (content: [])', () => {
    service.getBooks().subscribe(res => {
      expect(res).toEqual(mockEmptyPage);
      expect(res.empty).toBeTrue();
      expect(res.content.length).toBe(0);
    });

    const req = httpMock.expectOne(request => request.url === '/api/books');
    expect(req.request.method).toBe('GET');
    req.flush(mockEmptyPage);
  });
});