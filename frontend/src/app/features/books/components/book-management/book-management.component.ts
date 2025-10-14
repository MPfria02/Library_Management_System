import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, ViewChild, inject, signal } from '@angular/core';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { BookAdminResponse, BookSearchFilters, PageResponse } from '../../models/book.model';
import { AdminBookService } from '../../services/admin-book.service';
import { BookDeleteDialogComponent } from '../book-delete-dialog/book-delete-dialog.component';
import { BookFiltersComponent } from '../book-filters/book-filters.component';

@Component({
  selector: 'app-book-management',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatIconModule,
    MatButtonModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatDialogModule,
    DatePipe
],
  templateUrl: './book-management.component.html',
  styleUrls: ['./book-management.component.scss'],
})
export class BookManagementComponent implements OnInit {
  books = signal<BookAdminResponse[]>([]);
  loading = signal<boolean>(false);
  currentPage = signal<number>(0);
  totalPages = signal<number>(0);
  totalElements = signal<number>(0);
  currentFilters = signal<BookSearchFilters>({});
  readonly displayedColumns = ['isbn', 'title', 'author', 'genre', 'totalCopies', 'availableCopies', 'publicationDate', 'actions'];
  dataSource = new MatTableDataSource<BookAdminResponse>([]);
  readonly pageSize = 30;

  private adminBookService = inject(AdminBookService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  ngOnInit(): void {
    this.loadBooks();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  loadBooks(): void {
    this.loading.set(true);
    this.adminBookService.getBooks(this.currentPage(), this.pageSize, this.currentFilters()).subscribe({
      next: (page: PageResponse<BookAdminResponse>) => {
        this.books.set(page.content);
        this.totalPages.set(page.totalPages);
        this.totalElements.set(page.totalElements);
        this.dataSource.data = page.content;
        this.loading.set(false);
      },
      error: (err: unknown) => {
        console.error('Failed to load books', err);
        this.loading.set(false);
        this.snackBar.open('Failed to load books. Please try again.', 'Close', { duration: 3000 });
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.currentPage.set(event.pageIndex);
    this.loadBooks();
  }

  openFilterDialog(): void {
    const dialogRef = this.dialog.open(BookFiltersComponent, {
      width: '450px',
      data: this.currentFilters(),
    });
    dialogRef.afterClosed().subscribe((filters: BookSearchFilters | undefined) => {
      if (filters !== undefined) {
        this.currentFilters.set(filters);
        this.currentPage.set(0);
        this.loadBooks();
      }
    });
  }

  addNewBook(): void {
    this.router.navigate(['/admin/books/new']);
  }

  navigateToUsers(): void {
    this.router.navigate(['/admin/users']);
  }

  editBook(id: number): void {
    this.router.navigate(['/admin/books', id, 'edit']);
  }

  deleteBook(book: BookAdminResponse): void {
    const dialogRef = this.dialog.open(BookDeleteDialogComponent, {
      width: '400px',
      data: book,
    });
    dialogRef.afterClosed().subscribe((confirm: boolean | null) => {
      if (confirm) {
        this.adminBookService.deleteBook(book.id).subscribe({
          next: () => {
            this.snackBar.open('Book deleted successfully', 'Close', { duration: 2500 });
            this.loadBooks();
          },
          error: (err: unknown) => {
            console.error('Failed to delete book', err);
            this.snackBar.open('Failed to delete book. Please try again.', 'Close', { duration: 3000 });
          }
        });
      }
    });
  }
}


