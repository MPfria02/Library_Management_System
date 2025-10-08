import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BookResponse } from '../models/book.model';

@Injectable({ providedIn: 'root' })
export class InventoryService {
  private readonly apiUrl = '/api/inventory/books';
  private http = inject(HttpClient);

  /**
   * Borrow a book by its ID.
   * Decrements availableCopies by 1 on success.
   * @param bookId The ID of the book to borrow
   * @returns Observable of the updated BookResponse
   */
  borrowBook(bookId: number): Observable<BookResponse> {
    return this.http.post<BookResponse>(`${this.apiUrl}/${bookId}/borrow`, null);
  }

  /**
   * Return a book by its ID.
   * Increments availableCopies by 1 on success.
   * @param bookId The ID of the book to return
   * @returns Observable of the updated BookResponse
   */
  returnBook(bookId: number): Observable<BookResponse> {
    return this.http.post<BookResponse>(`${this.apiUrl}/${bookId}/return`, null);
  }
}


