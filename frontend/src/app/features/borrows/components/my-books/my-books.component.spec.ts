import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';

import { MyBooksComponent } from './my-books.component';
import { BorrowRecordService } from '../../services/borrow-record.service';
import { Router } from '@angular/router';
import { asyncScheduler, of, scheduled } from 'rxjs';
import { BorrowRecordResponse, BorrowStatus } from '../../models/borrow-record.model';
import { PageResponse } from '../../../books/models/book.model';
import { createMockBorrowRecord, createMockPageResponse, createMockReturnedBorrow } from '../../testing/borrow-record.test-helpers';
import { By } from '@angular/platform-browser';
import { PageEvent } from '@angular/material/paginator';

describe('MyBooksComponent', () => {
  let component: MyBooksComponent;
  let fixture: ComponentFixture<MyBooksComponent>;
  let mockBorrowRecordService: jasmine.SpyObj<BorrowRecordService>
  let mockRouter: jasmine.SpyObj<Router>

  beforeEach(async () => {
    mockBorrowRecordService = jasmine.createSpyObj('BorrowRecordService', ['getBorrowRecords']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [MyBooksComponent],
      providers: [
        { provide: BorrowRecordService, useValue: mockBorrowRecordService },
        { provide: Router, useValue: mockRouter },
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MyBooksComponent);
    component = fixture.componentInstance;
  });

  describe('Initialization', () => {
    it('should create component', () => {
           expect(component).toBeTruthy();
    });

    it('should load borrow records on init', () => {
      const mockRecords = [createMockBorrowRecord()];
      const mockResponse = createMockPageResponse(mockRecords);
      mockBorrowRecordService.getBorrowRecords.and.returnValue(of(mockResponse));

       // Act
      component.ngOnInit();

      // Assert
      expect(mockBorrowRecordService.getBorrowRecords).toHaveBeenCalledWith(0, 10, BorrowStatus.BORROWED);
      expect(component.borrowRecords().length).toBe(1);
      expect(component.loading()).toBe(false);
    });

    it('should set loading to true while fetching records', fakeAsync(() => {
      // Arrange
      mockBorrowRecordService.getBorrowRecords.and.returnValue(scheduled([createMockPageResponse([])], asyncScheduler));
      
      // Act
      component.ngOnInit();

      // Assert (before subscription completes)
      expect(component.loading()).toBe(true);

      tick();

      // Assert (after subscription completes)
      expect(component.loading()).toBe(false);
    }));
  })
  
  describe('Empty State', () => {
    it('should display empty state when no borrow records exist', () => {
      // Arrange
      mockBorrowRecordService.getBorrowRecords.and.returnValue(
        of(createMockPageResponse([]))
      );

      // Act
      fixture.detectChanges();

      // Assert
      const emptyState = fixture.debugElement.query(By.css('.empty-state'));
      expect(emptyState).not.toBeNull();
    });

    it('should show "No books borrowed yet" message when filter is BORROWED', fakeAsync(() => {
      // Arrange
      mockBorrowRecordService.getBorrowRecords.and.returnValue(
        of(createMockPageResponse([]))
      );
      component.selectedFilter.set(BorrowStatus.BORROWED);

      // Act
      fixture.detectChanges();

      // Assert
      const emptyState = fixture.debugElement.query(By.css('.empty-state'));
      expect(emptyState).not.toBeNull();
      expect(emptyState.nativeElement.textContent).toContain('No books borrowed yet');
    }));

    it('should show "No borrow history" message when filter is RETURNED', () => {
      // Arrange
      mockBorrowRecordService.getBorrowRecords.and.returnValue(
        of(createMockPageResponse([]))
      );
      component.selectedFilter.set(BorrowStatus.RETURNED);

      // Act
      fixture.detectChanges();

      // Assert
      const emptyState = fixture.debugElement.query(By.css('.empty-state'));
      expect(emptyState).not.toBeNull();
      expect(emptyState.nativeElement.textContent).toContain('No borrow history');
    });
  });

  describe('Filter Toggle', () => {
    beforeEach(() => {
      mockBorrowRecordService.getBorrowRecords.and.returnValue(
        of(createMockPageResponse([]))
      );
      component.ngOnInit();
      fixture.detectChanges();
    });

    it('should display filter toggle with three options', () => {
      const toggleButtons = fixture.debugElement.queryAll(By.css('mat-button-toggle'));
      expect(toggleButtons.length).toBe(2);
    });

    it('should set filter to BORROWED when Active button clicked', () => {
      // Arrange
      const activeButton = fixture.debugElement.queryAll(By.css('mat-button-toggle'))[0];

      // Act
      activeButton.nativeElement.click();
      fixture.detectChanges();

      // Assert
      expect(component.selectedFilter()).toBe(BorrowStatus.BORROWED);
    });

    it('should call loadBorrowRecords when filter changes', () => {
      // Arrange
      spyOn(component, 'loadBorrowRecords');

      // Act
      component.onFilterChange(BorrowStatus.BORROWED);

      // Assert
      expect(component.loadBorrowRecords).toHaveBeenCalled();
    });

    it('should reset to page 0 when filter changes', () => {
      // Arrange
      component.currentPage.set(3);  // Start on page 3
      fixture.detectChanges();

      // Act
      component.onFilterChange(BorrowStatus.BORROWED);
      fixture.detectChanges();

      // Assert
      expect(component.currentPage()).toBe(0);
    });

    it('should pass status parameter to service when filtering by BORROWED', () => {
      // Arrange
      component.selectedFilter.set(BorrowStatus.BORROWED);

      // Act
      component.loadBorrowRecords();

      // Assert
      expect(mockBorrowRecordService.getBorrowRecords).toHaveBeenCalledWith(
        0, 
        10, 
        BorrowStatus.BORROWED
      );
    });
  });

  describe('Active Count Computed Signal', () => {
    it('should compute active count correctly with mixed statuses', () => {
      // Arrange
      const mockRecords = [
        createMockBorrowRecord({ status: BorrowStatus.BORROWED }),
        createMockBorrowRecord({ status: BorrowStatus.BORROWED }),
        createMockReturnedBorrow(),
        createMockReturnedBorrow()
      ];
      mockBorrowRecordService.getBorrowRecords.and.returnValue(
        of(createMockPageResponse(mockRecords))
      );

      // Act
      component.ngOnInit();
      TestBed.flushEffects();

      // Assert
      expect(component.activeCount()).toBe(2);
    });

    it('should update active count when records change', () => {
      // Arrange - Start with 2 active borrows
      const initialRecords = [
        createMockBorrowRecord({ status: BorrowStatus.BORROWED }),
        createMockBorrowRecord({ status: BorrowStatus.BORROWED })
      ];
      mockBorrowRecordService.getBorrowRecords.and.returnValue(
        of(createMockPageResponse(initialRecords))
      );
      component.ngOnInit();
      TestBed.flushEffects();

      expect(component.activeCount()).toBe(2);

      // Act - Update to 1 active borrow
      const updatedRecords = [
        createMockBorrowRecord({ status: BorrowStatus.BORROWED })
      ];
      component.borrowRecords.set(updatedRecords);
      TestBed.flushEffects();

      // Assert
      expect(component.activeCount()).toBe(1);
    });

    it('should return 0 when no active borrows', () => {
      // Arrange
      const mockRecords = [
        createMockReturnedBorrow(),
        createMockReturnedBorrow()
      ];
      mockBorrowRecordService.getBorrowRecords.and.returnValue(
        of(createMockPageResponse(mockRecords))
      );

      // Act
      component.ngOnInit();
      TestBed.flushEffects();

      // Assert
      expect(component.activeCount()).toBe(0);
    });
  });

   describe('Navigation', () => {
    beforeEach(() => {
      mockBorrowRecordService.getBorrowRecords.and.returnValue(
        of(createMockPageResponse([]))
      );
      component.ngOnInit();
      fixture.detectChanges();
    });

    it('should navigate to book details when viewBookDetails called', () => {
      // Arrange
      const bookId = 42;

      // Act
      component.viewBookDetails(bookId);

      // Assert
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/books', 42]);
    });

    it('should navigate to catalog when goToCatalog called', () => {
      // Act
      component.goToCatalog();

      // Assert
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/dashboard']);
    });

    // it('should handle viewDetails event from borrow card', () => {
    //   // Arrange
    //   const mockRecord = createMockBorrowRecord({ bookId: 99 });
    //   mockBorrowRecordService.getBorrowRecords.and.returnValue(
    //     of(createMockPageResponse([mockRecord]))
    //   );
    //   component.ngOnInit();
    //   fixture.detectChanges();
    //   spyOn(component, 'viewBookDetails');

    //   // Act
    //   const borrowCard = fixture.debugElement.query(By.css('app-borrow-card'));
    //   borrowCard.componentInstance.viewDetails.emit(99);

    //   // Assert
    //   expect(component.viewBookDetails).toHaveBeenCalledWith(99);
    // });
  });

  describe('Pagination', () => {
    beforeEach(() => {
      const mockRecords = Array.from({ length: 10 }, (_, i) => 
        createMockBorrowRecord({ id: i + 1 })
      );
      mockBorrowRecordService.getBorrowRecords.and.returnValue(
        of(createMockPageResponse(mockRecords, { totalPages: 3, totalElements: 30 }))
      );
      component.ngOnInit();
      fixture.detectChanges();
    });

    it('should display paginator when totalPages > 1', () => {
      const paginator = fixture.debugElement.query(By.css('mat-paginator'));
      expect(paginator).toBeTruthy();
    });

    it('should NOT display paginator when totalPages <= 1', () => {
      // Arrange
      mockBorrowRecordService.getBorrowRecords.and.returnValue(
        of(createMockPageResponse([], { totalPages: 1 }))
      );
      component.loadBorrowRecords();
      fixture.detectChanges();

      // Assert
      const paginator = fixture.debugElement.query(By.css('mat-paginator'));
      expect(paginator).toBeNull();
    });

    it('should update currentPage when pagination changes', () => {
      // Arrange
      const pageEvent: PageEvent = {
        pageIndex: 2,
        pageSize: 10,
        length: 30
      };

      // Act
      component.onPageChange(pageEvent);

      // Assert
      expect(component.currentPage()).toBe(2);
    });

    it('should update pageSize when user changes page size', () => {
      // Arrange
      const pageEvent: PageEvent = {
        pageIndex: 0,
        pageSize: 20,
        length: 30
      };

      // Act
      component.onPageChange(pageEvent);

      // Assert
      expect(component.pageSize()).toBe(20);
    });

    it('should reload borrow records when page changes', () => {
      // Arrange
      const pageEvent: PageEvent = {
        pageIndex: 1,
        pageSize: 10,
        length: 30
      };
      mockBorrowRecordService.getBorrowRecords.calls.reset();

      // Act
      component.onPageChange(pageEvent);

      // Assert
      expect(mockBorrowRecordService.getBorrowRecords).toHaveBeenCalledWith(1, 10, BorrowStatus.BORROWED);
    });
  });
});
