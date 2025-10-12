import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { BookCreationRequest, BookResponse, PageResponse, BookAdminResponse, BookSearchFilters } from '../models/book.model';

@Injectable({ providedIn: 'root' })
export class AdminBookService {
  private readonly apiUrl = '/api/admin/books';
  private http = inject(HttpClient);

  /** Create a new book */
  createBook(request: BookCreationRequest): Observable<BookAdminResponse> {
    return this.http.post<BookAdminResponse>(this.apiUrl, request);
  }

  /** Update an existing book by id */
  updateBook(id: number, request: BookCreationRequest): Observable<BookAdminResponse> {
    return this.http.put<BookAdminResponse>(`${this.apiUrl}/${id}`, request);
  }

  /** Delete a book by id */
  deleteBook(id: number): Observable<void> {
    return this.http.delete(`${this.apiUrl}/${id}`, { observe: 'response' }).pipe(map(() => undefined as void));
  }

  /** Get paginated books for admin */
  getBooks(page: number = 0, size: number = 30, filters?: BookSearchFilters): Observable<PageResponse<BookAdminResponse>> {
    let params: any = { page, size, sortBy: 'title', sortDir: 'asc' };
    if (filters) {
      if (filters.searchTerm) params.searchTerm = filters.searchTerm;
      if (filters.genre) params.genre = filters.genre;
      if (filters.availableOnly !== undefined) params.availableOnly = filters.availableOnly;
    }
    return this.http.get<PageResponse<BookAdminResponse>>(this.apiUrl, { params });
  }

  /** Get single book by ID for admin */
  getBookById(id: number): Observable<BookAdminResponse> {
    return this.http.get<BookAdminResponse>(`${this.apiUrl}/${id}`);
  }
}


