import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DatePipe } from '@angular/common';
import { BookResponse } from '../../models/book.model';
import { BookService } from '../../services/book.service';
import { InventoryService } from '../../services/inventory.service';
import { BorrowRecordResponse } from '../../../borrows/models/borrow-record.model';

@Component({
  selector: 'app-book-details',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatChipsModule,
    MatSnackBarModule,
    MatTooltipModule,
    DatePipe,
  ],
  templateUrl: './book-details.component.html',
  styleUrls: ['./book-details.component.scss'],
})
export class BookDetailsComponent implements OnInit {
  book = signal<BookResponse | null>(null);
  borrowRecord = signal<BorrowRecordResponse | null>(null);
  loading = signal<boolean>(true);
  actionInProgress = signal<boolean>(false);
  hasBorrowed = signal<boolean>(false);
  checkingStatus = signal<boolean>(false);

  private bookService = inject(BookService);
  private inventoryService = inject(InventoryService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  ngOnInit(): void {
    const bookIdParam = Number(this.route.snapshot.paramMap.get('id'));
    if (isNaN(bookIdParam)) {
      this.snackBar.open('Invalid book ID', 'Close', { duration: 3000 });
      this.router.navigate(['/dashboard']);
      return;
    }
    this.loadBook(bookIdParam);
    this.checkBorrowStatus(bookIdParam); 
  }
  
  checkBorrowStatus(bookId: number): void {
    this.checkingStatus.set(true);
    this.inventoryService.checkBorrowStatus(bookId).subscribe({
      next: (response) => {
        this.hasBorrowed.set(response.borrowed);
        this.checkingStatus.set(false);
      },
      error: (error) => {
        console.error('Error checking borrow status:', error);
        this.checkingStatus.set(false);
      }
    });
  }

  /** Load book by ID from route parameters */
  private loadBook(bookIdParam: number): void {
    this.loading.set(true);
    this.bookService.getBookById(bookIdParam).subscribe({
      next: (book: BookResponse) => {
        this.book.set(book);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load book', err);
        this.loading.set(false);
        
        if (err.status === 404) {
          this.snackBar.open('Book not found', 'Close', { duration: 3000 });
          this.router.navigate(['/dashboard']);
        } else {
          this.snackBar.open('Failed to load book details', 'Close', { duration: 3000 });
        }
      },
    });
  }

  /** Handle borrow book action */
  borrowBook(): void {
    const book = this.book();
    if (!book || this.actionInProgress()) {
      return;
    }

    this.actionInProgress.set(true);
    this.inventoryService.borrowBook(book.id).subscribe({
      next: (borrowRecord: BorrowRecordResponse) => {
        
        this.borrowRecord.set(borrowRecord);

        this.book.update(current => {
        if (!current) return current;
          return {
            ...current,
            availableCopies: current.availableCopies - 1
          };
        });

        this.hasBorrowed.set(true);  
        this.actionInProgress.set(false);
        this.snackBar.open('Book borrowed successfully', 'Close', { duration: 3000 });
      },
      error: (err) => {
        console.error('Failed to borrow book', err);
        this.actionInProgress.set(false);
        
        const errorMessage = err.error?.message || 'Failed to borrow book';
        this.snackBar.open(errorMessage, 'Close', { duration: 3000 });
      },
    });
  }

  returnBook(): void {
    const book = this.book();
    if (!book || this.actionInProgress()) {
      return;
    }

    this.actionInProgress.set(true);
    this.inventoryService.returnBook(book.id).subscribe({
      next: (borrowRecord: BorrowRecordResponse) => {  // 🔄 Now receives BorrowRecordResponse
        // Update local book state
        this.book.update(b => b ? {
          ...b,
          availableCopies: b.availableCopies + 1
        } : null);
        
        this.hasBorrowed.set(false);  // 🆕 Update borrow status
        this.actionInProgress.set(false);
        
        this.snackBar.open('Book returned successfully', 'Close', { duration: 3000 });
      },
      error: (error) => {
        console.error('Error returning book:', error);
        this.actionInProgress.set(false);
        this.snackBar.open(
          error.error?.message || 'Failed to return book',
          'Close',
          { duration: 3000 }
        );
      }
    });
  }

  /** Navigate back to catalog */
  backToCatalog(): void {
    this.router.navigate(['/dashboard']);
  }

  /** See book in My-Books page */
  viewInMyBooks(): void {
    this.router.navigate(['/dashboard/my-books']);
  }

  /** Check if book is available for borrowing */
  get canBorrow(): boolean {
    const book = this.book();
    return book !== null && 
           book.availableCopies > 0 && 
           !this.hasBorrowed();  // 🆕 Can't borrow if already borrowed
  }

  /** Get availability text */
  getAvailabilityText(): string {
    const book = this.book();
    if (!book) return '';
    
    return `${book.availableCopies} copies available`;
  }

   get showReturnButton(): boolean {
    return this.hasBorrowed();
  }
}
