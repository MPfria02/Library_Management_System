// Matches backend BorrowStatus enum
export enum BorrowStatus {
  BORROWED = 'BORROWED',
  RETURNED = 'RETURNED'
}

// Matches backend BorrowRecordResponse DTO
export interface BorrowRecordResponse {
  id: number;
  bookId: number;
  bookTitle: string;
  bookAuthor: string;
  bookIsbn: string;
  status: BorrowStatus;
  borrowDate: string;  // ISO date string: "2025-10-14"
  dueDate: string;     // ISO date string: "2025-10-21"
  returnDate: string | null;  // ISO date or null if not returned
  isOverdue: boolean;
}

// For filter toggle UI
export interface BorrowFilter {
  status: BorrowStatus; 
}

// For status check endpoint response
export interface BorrowStatusResponse {
  borrowed: boolean;
}