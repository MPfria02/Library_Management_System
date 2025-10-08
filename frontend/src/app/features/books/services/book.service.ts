import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BookGenre, BookResponse, BookSearchFilters, PageResponse } from '../models/book.model';

@Injectable({ providedIn: 'root' })
export class BookService {
  private readonly apiUrl = '/api/books';
  private http = inject(HttpClient);

  /**
   * Fetch paginated books from the backend.
   * Always includes page, size, sortBy ('title'), and sortDir ('asc').
   * Conditionally includes searchTerm, genre, and availableOnly when provided.
   *
   * @param page Page index (0-based). Defaults to 0.
   * @param size Page size. Defaults to 12.
   * @param filters Optional filters for search and availability.
   * @returns Observable of a PageResponse with BookResponse items.
   */
  getBooks(
    page: number = 0,
    size: number = 12,
    filters?: BookSearchFilters
  ): Observable<PageResponse<BookResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', 'title')
      .set('sortDir', 'asc');

    if (filters?.searchTerm) {
      params = params.set('searchTerm', filters.searchTerm);
    }

    if (filters?.genre) {
      params = params.set('genre', filters.genre);
    }

    if (filters?.availableOnly) {
      params = params.set('availableOnly', 'true');
    }

    return this.http.get<PageResponse<BookResponse>>(this.apiUrl, { params });
  }

  /**
   * Fetch a single book by its ID.
   * @param id Book identifier
   * @returns Observable of BookResponse
   */
  getBookById(id: number): Observable<BookResponse> {
    return this.http.get<BookResponse>(`${this.apiUrl}/${id}`);
  }

  /**
   * Fetch the list of available genres.
   * @returns Observable of an array of BookGenre values
   */
  getAvailableGenres(): Observable<BookGenre[]> {
    return this.http.get<BookGenre[]>(`${this.apiUrl}/genres`);
  }
}