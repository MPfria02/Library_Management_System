import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BorrowRecordResponse, BorrowStatusResponse } from '../../borrows/models/borrow-record.model';

@Injectable({ providedIn: 'root' })
export class InventoryService {
  private readonly apiUrl = '/api/inventory/books';
  private http = inject(HttpClient);

  /**
   * Borrow a book
   * Returns borrow record
   */
  borrowBook(bookId: number): Observable<BorrowRecordResponse> {
    return this.http.post<BorrowRecordResponse>(
      `${this.apiUrl}/${bookId}/borrow`,
      null
    );
  }


    /**
   * Return a borrowed book
   * Returns updated borrow record with return date
   */
  returnBook(bookId: number): Observable<BorrowRecordResponse> {
    return this.http.post<BorrowRecordResponse>(
      `${this.apiUrl}/${bookId}/return`,
      null
    );
  }
  
   /**
   * Check if current user has borrowed a specific book
   * Used to show correct button (Borrow vs Return) on book details page
   */
  checkBorrowStatus(bookId: number): Observable<BorrowStatusResponse> {
    return this.http.get<BorrowStatusResponse>(
      `${this.apiUrl}/${bookId}/status`
    );
  }
}


