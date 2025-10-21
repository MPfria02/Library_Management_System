import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import { Router } from '@angular/router';
import { BorrowRecordResponse, BorrowStatus } from '../../models/borrow-record.model';
import { BorrowRecordService } from '../../services/borrow-record.service';
import { BorrowCardComponent } from '../borrow-card/borrow-card.component';

@Component({
  selector: 'app-my-books',
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatButtonToggleModule,
    MatIconModule,
    BorrowCardComponent
  ],
  templateUrl: './my-books.component.html',
  styleUrls: ['./my-books.component.scss']
})
export class MyBooksComponent {
  // Services
  private borrowRecordService = inject(BorrowRecordService);
  private router = inject(Router);

  // State signals
  borrowRecords = signal<BorrowRecordResponse[]>([]);
  loading = signal<boolean>(false);
  currentPage = signal<number>(0);
  totalPages = signal<number>(0);
  totalElements = signal<number>(0);
  pageSize = signal<number>(10);
  
  // Filter state
  selectedFilter = signal<BorrowStatus>(BorrowStatus.BORROWED);
  
  // Computed signals
  activeCount = computed(() => 
    this.borrowRecords().filter(r => r.status === BorrowStatus.BORROWED).length
  );
  
  isEmpty = computed(() => 
    !this.loading() && this.borrowRecords().length === 0
  );

  ngOnInit(): void {
    this.loadBorrowRecords();
  }

  loadBorrowRecords(): void {
    this.loading.set(true);
    
    // Determine status param based on filter
    const statusParam = this.selectedFilter()
    
    this.borrowRecordService
      .getBorrowRecords(this.currentPage(), this.pageSize(), statusParam)
      .subscribe({
        next: (response) => {
          this.borrowRecords.set(response.content);
          this.totalPages.set(response.totalPages);
          this.totalElements.set(response.totalElements);
          this.loading.set(false);
        },
        error: (error) => {
          console.error('Error loading borrow records:', error);
          this.loading.set(false);
          // TODO: Show error snackbar
        }
      });
  }

  onFilterChange(value: string): void {
    const filter = value as BorrowStatus;
    this.selectedFilter.set(filter);
    this.currentPage.set(0);  // Reset to first page when filter changes
    this.loadBorrowRecords();
  }

  onPageChange(event: PageEvent): void {
    this.currentPage.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadBorrowRecords();
  }

  viewBookDetails(bookId: number): void {
    this.router.navigate(['/books', bookId]);
  }

  goToCatalog(): void {
    this.router.navigate(['/dashboard']);
  }
}
