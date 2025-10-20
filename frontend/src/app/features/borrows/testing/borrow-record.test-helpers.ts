import { BorrowRecordResponse, BorrowStatus } from '../models/borrow-record.model';
import { PageResponse } from '../../books/models/book.model';

/**
 * Factory function to create mock BorrowRecordResponse
 * Provides sensible defaults, allows overrides
 */
export function createMockBorrowRecord(
  overrides?: Partial<BorrowRecordResponse>
): BorrowRecordResponse {
  const today = new Date();
  const borrowDate = new Date(today);
  borrowDate.setDate(today.getDate() - 3); // 3 days ago
  
  const dueDate = new Date(borrowDate);
  dueDate.setDate(borrowDate.getDate() + 7); // 7 days after borrow
  
  return {
    id: 1,
    bookId: 10,
    bookTitle: 'Test Book Title',
    bookAuthor: 'Test Author',
    bookIsbn: '9780000000000',
    status: BorrowStatus.BORROWED,
    borrowDate: borrowDate.toISOString().split('T')[0], // "2025-10-11"
    dueDate: dueDate.toISOString().split('T')[0],       // "2025-10-18"
    returnDate: null,
    isOverdue: false,
    ...overrides
  };
}

/**
 * Create mock overdue borrow record
 */
export function createMockOverdueBorrow(
  overrides?: Partial<BorrowRecordResponse>
): BorrowRecordResponse {
  const today = new Date();
  const borrowDate = new Date(today);
  borrowDate.setDate(today.getDate() - 15); // 15 days ago
  
  const dueDate = new Date(borrowDate);
  dueDate.setDate(borrowDate.getDate() + 7); // Due 8 days ago
  
  return createMockBorrowRecord({
    borrowDate: borrowDate.toISOString().split('T')[0],
    dueDate: dueDate.toISOString().split('T')[0],
    returnDate: null,
    status: BorrowStatus.BORROWED,
    isOverdue: true,
    ...overrides
  });
}

/**
 * Create mock returned borrow record
 */
export function createMockReturnedBorrow(
  overrides?: Partial<BorrowRecordResponse>
): BorrowRecordResponse {
  const today = new Date();
  const borrowDate = new Date(today);
  borrowDate.setDate(today.getDate() - 20); // 20 days ago
  
  const dueDate = new Date(borrowDate);
  dueDate.setDate(borrowDate.getDate() + 7);
  
  const returnDate = new Date(borrowDate);
  returnDate.setDate(borrowDate.getDate() + 5); // Returned on time (2 days early)
  
  return createMockBorrowRecord({
    borrowDate: borrowDate.toISOString().split('T')[0],
    dueDate: dueDate.toISOString().split('T')[0],
    returnDate: returnDate.toISOString().split('T')[0],
    status: BorrowStatus.RETURNED,
    isOverdue: false,
    ...overrides
  });
}

/**
 * Create mock paginated response matching our simplified PageResponse interface
 * 
 * @param content Array of items for current page
 * @param overrides Optional field overrides for testing edge cases
 */
export function createMockPageResponse<T>(
  content: T[],
  overrides?: Partial<PageResponse<T>>
): PageResponse<T> {
  const defaultSize = 10;
  const defaultPage = 0;
  
  return {
    content,
    totalPages: Math.ceil((overrides?.totalElements ?? content.length) / defaultSize),
    totalElements: content.length,
    size: defaultSize,
    number: defaultPage,
    first: defaultPage === 0,
    last: true,  // Assume single page by default
    empty: content.length === 0,
    ...overrides  // Allow test overrides
  };
}
