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
  loading = signal<boolean>(true);
  actionInProgress = signal<boolean>(false);

  private bookService = inject(BookService);
  private inventoryService = inject(InventoryService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  ngOnInit(): void {
    this.loadBook();
  }

  /** Load book by ID from route parameters */
  private loadBook(): void {
    const bookIdParam = this.route.snapshot.paramMap.get('id');
    
    if (!bookIdParam) {
      this.snackBar.open('Invalid book ID', 'Close', { duration: 3000 });
      this.router.navigate(['/dashboard']);
      return;
    }

    const bookId = Number(bookIdParam);
    if (isNaN(bookId)) {
      this.snackBar.open('Invalid book ID', 'Close', { duration: 3000 });
      this.router.navigate(['/dashboard']);
      return;
    }

    this.loading.set(true);
    this.bookService.getBookById(bookId).subscribe({
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
      next: (updatedBook: BookResponse) => {
        this.book.set(updatedBook);
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

  /** Navigate back to catalog */
  backToCatalog(): void {
    this.router.navigate(['/dashboard']);
  }

  /** Check if book is available for borrowing */
  isBookAvailable(): boolean {
    const book = this.book();
    return book ? book.availableCopies > 0 : false;
  }

  /** Get availability text */
  getAvailabilityText(): string {
    const book = this.book();
    if (!book) return '';
    
    return `${book.availableCopies} copies available`;
  }
}
