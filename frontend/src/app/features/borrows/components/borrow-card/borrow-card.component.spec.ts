import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BorrowCardComponent } from './borrow-card.component';
import { BorrowStatus } from '../../models/borrow-record.model';
import { 
  createMockBorrowRecord, 
  createMockOverdueBorrow, 
  createMockReturnedBorrow 
} from '../../testing/borrow-record.test-helpers';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

describe('BorrowCardComponent', () => {
  let component: BorrowCardComponent;
  let fixture: ComponentFixture<BorrowCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BorrowCardComponent] // Standalone component
    }).compileComponents();

    fixture = TestBed.createComponent(BorrowCardComponent);
    component = fixture.componentInstance;
  });

  describe('Component Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should require borrowRecord input', () => {
      // borrowRecord is marked as required, should fail if not provided
      expect(component.borrowRecord).toBeUndefined();
    });
  });

  describe('Displaying Active Borrow', () => {
    beforeEach(() => {
      component.borrowRecord = createMockBorrowRecord();
      fixture.detectChanges();
    });

    it('should display book title', () => {
      const titleElement = fixture.debugElement.query(By.css('.book-title'));
      expect(titleElement.nativeElement.textContent).toBe('Test Book Title');
    });

    it('should display book author', () => {
      const authorElement = fixture.debugElement.query(By.css('.book-author'));
      expect(authorElement.nativeElement.textContent).toContain('Test Author');
    });

    it('should display book ISBN', () => {
      const isbnElement = fixture.debugElement.query(By.css('.book-isbn'));
      expect(isbnElement.nativeElement.textContent).toContain('9780000000000');
    });

    it('should display "Active" status chip', () => {
      const chipElement = fixture.debugElement.query(By.css('.status-chip.active'));
      expect(chipElement).toBeTruthy();
      expect(chipElement.nativeElement.textContent).toContain('Active');
    });

    it('should display borrow date formatted correctly', () => {
      const dateElement = fixture.debugElement.query(
        By.css('.dates .date-row:first-child span:last-child')
      );
      // Date format: "Oct 11, 2025" (depends on your DatePipe locale)
      expect(dateElement.nativeElement.textContent).toMatch(/\w{3} \d{1,2}, \d{4}/);
    });

    it('should display due date (not return date) for active borrow', () => {
      const dueDateLabel = fixture.debugElement.query(
        By.css('.dates .date-row:nth-child(2) .label')
      );
      expect(dueDateLabel.nativeElement.textContent).toContain('Due:');
    });

    it('should NOT display return date for active borrow', () => {
      const returnDateLabel = fixture.debugElement.queryAll(By.css('.label'))
        .find(el => el.nativeElement.textContent.includes('Returned:'));
      expect(returnDateLabel).toBeUndefined();
    });
  });

  describe('Displaying Overdue Borrow', () => {
    beforeEach(() => {
      component.borrowRecord = createMockOverdueBorrow();
      fixture.detectChanges();
    });

    it('should apply "overdue" CSS class to due date row', () => {
      const dueDateRow = fixture.debugElement.query(By.css('.date-row.overdue'));
      expect(dueDateRow).toBeTruthy();
    });

    it('should display warning icon for overdue borrow', () => {
      const warningIcon = fixture.debugElement.query(By.css('.overdue-icon'));
      expect(warningIcon).toBeTruthy();
      expect(warningIcon.nativeElement.textContent).toContain('warning');
    });
  });

  describe('Displaying Returned Borrow', () => {
    beforeEach(() => {
      component.borrowRecord = createMockReturnedBorrow();
      fixture.detectChanges();
    });

    it('should display "Returned" status chip', () => {
      const chipElement = fixture.debugElement.query(By.css('.status-chip.returned'));
      expect(chipElement).toBeTruthy();
      expect(chipElement.nativeElement.textContent).toContain('Returned');
    });

    it('should display return date (not due date) for returned borrow', () => {
      const returnDateLabel = fixture.debugElement.query(
        By.css('.dates .date-row:nth-child(2) .label')
      );
      expect(returnDateLabel.nativeElement.textContent).toContain('Returned:');
    });

    it('should NOT display due date for returned borrow', () => {
      const dueDateLabel = fixture.debugElement.queryAll(By.css('.label'))
        .find(el => el.nativeElement.textContent.includes('Due:'));
      expect(dueDateLabel).toBeUndefined();
    });

    it('should NOT display overdue indicator for returned borrow', () => {
      const overdueRow = fixture.debugElement.query(By.css('.date-row.overdue'));
      expect(overdueRow).toBeNull();
    });
  });

  describe('User Interactions', () => {
    beforeEach(() => {
      component.borrowRecord = createMockBorrowRecord({ bookId: 42 });
      fixture.detectChanges();
    });

    it('should emit viewDetails event with bookId when button clicked', (done) => {
      // Arrange
      component.viewDetails.subscribe((bookId: number) => {
        // Assert
        expect(bookId).toBe(42);
        done();
      });

      // Act
      const button = fixture.debugElement.query(By.css('button'));
      button.nativeElement.click();
    });

    it('should call onViewDetails() when button clicked', () => {
      // Arrange
      spyOn(component, 'onViewDetails');

      // Act
      const button = fixture.debugElement.query(By.css('button'));
      button.nativeElement.click();

      // Assert
      expect(component.onViewDetails).toHaveBeenCalled();
    });
  });
});