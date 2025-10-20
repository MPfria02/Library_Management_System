import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BorrowRecordResponse, BorrowStatus } from '../models/borrow-record.model';
import { PageResponse } from '../../books/models/book.model';

@Injectable({ providedIn: 'root' })
export class BorrowRecordService {
  private http = inject(HttpClient);
  private apiUrl = '/api/inventory/books';

  /**
   * Get current user's borrow records with optional status filter
   * 
   * @param page Page number (0-indexed)
   * @param size Items per page
   * @param status Filter by BORROWED, RETURNED, or null for all
   * @returns Observable of paginated borrow records
   */
  getBorrowRecords(
    page: number = 0,
    size: number = 10,
    status?: BorrowStatus | null
  ): Observable<PageResponse<BorrowRecordResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    // Only add status param if filtering (don't send 'ALL')
    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<PageResponse<BorrowRecordResponse>>(
      this.apiUrl, 
      { params }
    );
  }

  /**
   * Get only active borrows (status = BORROWED)
   * Convenience method for common use case
   */
  getActiveBorrows(page: number = 0, size: number = 10): Observable<PageResponse<BorrowRecordResponse>> {
    return this.getBorrowRecords(page, size, BorrowStatus.BORROWED);
  }

  /**
   * Get borrow history (status = RETURNED)
   * Convenience method for common use case
   */
  getBorrowHistory(page: number = 0, size: number = 10): Observable<PageResponse<BorrowRecordResponse>> {
    return this.getBorrowRecords(page, size, BorrowStatus.RETURNED);
  }
}