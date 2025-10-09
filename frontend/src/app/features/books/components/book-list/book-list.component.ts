import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { BookResponse, BookSearchFilters, PageResponse } from '../../models/book.model';
import { BookService } from '../../services/book.service';
import { BookFiltersComponent } from '../book-filters/book-filters.component';

@Component({
  selector: 'app-book-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatChipsModule,
    MatSnackBarModule,
    MatDialogModule,
  ],
  templateUrl: './book-list.component.html',
  styleUrls: ['./book-list.component.scss'],
})
export class BookListComponent implements OnInit {
  books = signal<BookResponse[]>([]);
  loading = signal<boolean>(false);
  currentPage = signal<number>(0);
  totalPages = signal<number>(0);
  totalElements = signal<number>(0);
  pageSize = 12;
  currentFilters = signal<BookSearchFilters>({});

  private bookService = inject(BookService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private destroyRef = inject(DestroyRef);
  private dialog = inject(MatDialog);

  ngOnInit(): void {
    this.loadBooks();
  }

  /** Load books for the current page and filters */
  loadBooks(): void {
    this.loading.set(true);
    this.bookService.getBooks(this.currentPage(), this.pageSize, this.currentFilters()).subscribe({
      next: (page: PageResponse<BookResponse>) => {
        this.books.set(page.content);
        this.totalPages.set(page.totalPages);
        this.totalElements.set(page.totalElements);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load books', err);
        this.loading.set(false);
        this.snackBar.open('Failed to load books', 'Close', { duration: 3000 });
      },
    });
  }

  /** Handle paginator page change */
  onPageChange(event: PageEvent): void {
    this.currentPage.set(event.pageIndex);
    this.loadBooks();
  }

  /** Navigate to book details page */
  viewBookDetails(bookId: number): void {
    this.router.navigate(['/books', bookId]);
  }

  /** Filter dialog for filtering books given a certain criteria */
  openFilterDialog(): void {
      const dialogRef = this.dialog.open(BookFiltersComponent, {
        width: '450px',
        data: this.currentFilters()
      });

      dialogRef.afterClosed().subscribe((filters: BookSearchFilters | undefined)  => {
        if (filters !== undefined) {
          this.currentFilters.set(filters);
          this.currentPage.set(0);
          this.loadBooks();
        }
      });
  }
}
