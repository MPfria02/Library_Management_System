import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ActivatedRoute, ActivatedRouteSnapshot, convertToParamMap, ParamMap, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError, from, asyncScheduler, scheduled } from 'rxjs';
import { BookDetailsComponent } from './book-details.component';
import { BookService } from '../../services/book.service';
import { InventoryService } from '../../services/inventory.service';
import { BookResponse, BookGenre } from '../../models/book.model';
import { By } from '@angular/platform-browser';
import { BorrowStatus } from '../../../borrows/models/borrow-record.model';
import { createMockBorrowRecord } from '../../../borrows/testing/borrow-record.test-helpers';

describe('BookDetailsComponent', () => {
  let component: BookDetailsComponent;
  let fixture: ComponentFixture<BookDetailsComponent>;
  let bookServiceSpy: jasmine.SpyObj<BookService>;
  let inventoryServiceSpy: jasmine.SpyObj<InventoryService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let bookId: number;

  const mockBook: BookResponse = {
    id: 5,
    title: 'Test Book',
    author: 'Test Author',
    description: 'A test book description',
    genre: BookGenre.FICTION,
    availableCopies: 3,
    publicationDate: '2023-01-15'
  };

  beforeEach(async () => {
    bookServiceSpy = jasmine.createSpyObj('BookService', ['getBookById']);
    inventoryServiceSpy = jasmine.createSpyObj('InventoryService', ['borrowBook', 'returnBook', 'checkBorrowStatus']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      imports: [BookDetailsComponent],
      providers: [
        { provide: BookService, useValue: bookServiceSpy },
        { provide: InventoryService, useValue: inventoryServiceSpy },
        { provide: ActivatedRoute, useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: '5' }),
            },
          },  
        },
        { provide: Router, useValue: routerSpy },
      ]
    })
    .overrideProvider(MatSnackBar, { useValue: snackBarSpy })
    .compileComponents();

    fixture = TestBed.createComponent(BookDetailsComponent);
    component = fixture.componentInstance;
  });

  describe('Initialization', () => {
    it('should create services', () => {
      expect(bookServiceSpy).toBeTruthy();
      expect(inventoryServiceSpy).toBeTruthy();
    });

    it('should inject mock dependencies', () => {
      console.log(component['snackBar'] === snackBarSpy);
      expect(component['bookService'] === bookServiceSpy);
      expect(component['inventoryService'] === inventoryServiceSpy);
      console.log(component['router'] === routerSpy);
    });

    it('should read id from route', () => {
      expect(component['route'].snapshot.paramMap.get('id')).toBe('5');
    });

    it('should load book on init', () => {
      bookId = Number(component['route'].snapshot.paramMap.get('id'));
      bookServiceSpy.getBookById.and.returnValue(of(mockBook));
      inventoryServiceSpy.checkBorrowStatus.and.returnValue(of({borrowed: false}));

      component.ngOnInit();

      expect(bookServiceSpy.getBookById).toHaveBeenCalled();
      expect(inventoryServiceSpy.checkBorrowStatus).toHaveBeenCalled();
    });
  });

  describe('Borrow Status Check (new)', () => {
    it('should set hasBorrowed to true when user has borrowed book', (done) => {
      // Arrange
      inventoryServiceSpy.checkBorrowStatus.and.returnValue(
        of({ borrowed: true })
      );

      // Act
      component.checkBorrowStatus(5);
      TestBed.flushEffects();

      // Assert
      setTimeout(() => {
        expect(component.hasBorrowed()).toBe(true);
        done();
      }, 0);
    });

    it('should set hasBorrowed to false when user has not borrowed book', (done) => {
      // Arrange
      inventoryServiceSpy.checkBorrowStatus.and.returnValue(
        of({ borrowed: false })
      );

      // Act
      component.checkBorrowStatus(5);
      TestBed.flushEffects();

      // Assert
      setTimeout(() => {
        expect(component.hasBorrowed()).toBe(false);
        done();
      }, 0);
    });

    it('should set checkingStatus to true while checking status', fakeAsync(() => {
      // Arrange
      inventoryServiceSpy.checkBorrowStatus.and.returnValue(scheduled([{borrowed : false}], asyncScheduler));

      // Act
      component.checkBorrowStatus(5);

      // Assert (before subscription completes)
      expect(component.checkingStatus()).toBe(true);

      tick();

      // Assert (after subscription completes)
      expect(component.checkingStatus()).toBe(false);
    }));

    it('should NOT show error to user if status check fails', (done) => {
      // Arrange
      inventoryServiceSpy.checkBorrowStatus.and.returnValue(
        throwError(() => new Error('Network error'))
      );

      // Act
      component.checkBorrowStatus(5);
      TestBed.flushEffects();

      // Assert
      setTimeout(() => {
        expect(component['snackBar'].open).not.toHaveBeenCalled();
        done();
      }, 0);
    });
  });

  describe('Borrow Button Behavior', () => {
    beforeEach(() => {
      bookServiceSpy.getBookById.and.returnValue(of(mockBook));
      component.checkingStatus.set(false);
    });

    it('should display "Borrow Book" button when user has not borrowed', () => {
      // Arrange
      inventoryServiceSpy.checkBorrowStatus.and.returnValue(of({borrowed : false}));
      component.actionInProgress.set(false);
      fixture.detectChanges();

      // Assert
      const borrowButton = fixture.debugElement.query(
        By.css('button[color="primary"]')
      );
      expect(borrowButton).not.toBeNull();
      expect(borrowButton.nativeElement.textContent).toContain('Borrow Book');
    });

    it('should display "Return Book" button when user has borrowed', () => {
      // Arrange
      inventoryServiceSpy.checkBorrowStatus.and.returnValue(of({borrowed : true}));
      component.actionInProgress.set(false);
      fixture.detectChanges();

      // Assert
      const returnButton = fixture.debugElement.query(
        By.css('button[color="accent"]')
      );
      expect(returnButton).not.toBeNull();
      expect(returnButton.nativeElement.textContent).toContain('Return Book');
    });

    it('should disable borrow button when user has already borrowed', () => {
      // Arrange
      inventoryServiceSpy.checkBorrowStatus.and.returnValue(of({borrowed : true}));
      component.actionInProgress.set(false);
      fixture.detectChanges();

      // Assert
      const borrowButton = fixture.debugElement.query(
        By.css('button[color="primary"]')
      );
      // Borrow button should not exist when hasBorrowed=true
      expect(borrowButton).toBeNull();
    });

    it('should show "View in My Books" link when user has borrowed', () => {
      // Arrange
      inventoryServiceSpy.checkBorrowStatus.and.returnValue(of({borrowed : true}));
      component.actionInProgress.set(false);
      fixture.detectChanges();

      // Assert
      const myBooksLink = fixture.debugElement.query(
        By.css('button[routerLink="/my-books"]')
      );
      expect(myBooksLink).toBeTruthy();
    });
  });

  describe('Borrow Book Action', () => {
    beforeEach(() => {
      bookServiceSpy.getBookById.and.returnValue(of(mockBook));
      inventoryServiceSpy.checkBorrowStatus.and.returnValue(of({borrowed : false}));
      fixture.detectChanges();
    });

    it('should call inventoryService.borrowBook with bookId', fakeAsync(() => {
      // Arrange
      const mockBorrowRecord = createMockBorrowRecord({ bookId: 5 });
      inventoryServiceSpy.borrowBook.and.returnValue(scheduled([mockBorrowRecord], asyncScheduler));

      // Act
      component.borrowBook();
      tick();

      // Assert
      expect(inventoryServiceSpy.borrowBook).toHaveBeenCalledWith(5);
    }));

    it('should update book availableCopies after successful borrow', (done) => {
      // Arrange
      const mockBorrowRecord = createMockBorrowRecord({ bookId: 5 });
      inventoryServiceSpy.borrowBook.and.returnValue(of(mockBorrowRecord));
      const initialCopies = component.book()!.availableCopies;

      // Act
      component.borrowBook();
      TestBed.flushEffects();

      // Assert
      setTimeout(() => {
        expect(component.book()!.availableCopies).toBe(initialCopies - 1);
        done();
      }, 0);
    });

    it('should set hasBorrowed to true after successful borrow', (done) => {
      // Arrange
      const mockBorrowRecord = createMockBorrowRecord({ bookId: 5 });
      inventoryServiceSpy.borrowBook.and.returnValue(of(mockBorrowRecord));

      // Act
      component.borrowBook();
      TestBed.flushEffects();

      // Assert
      setTimeout(() => {
        expect(component.hasBorrowed()).toBe(true);
        done();
      }, 0);
    });

    it('should display due date in success message', (done) => {
      // Arrange
      const mockBorrowRecord = createMockBorrowRecord({ 
        bookId: 5,
        dueDate: '2025-10-21'
      });
      inventoryServiceSpy.borrowBook.and.returnValue(of(mockBorrowRecord));

      // Act
      component.borrowBook();
      TestBed.flushEffects();

      // Assert
      setTimeout(() => {
        expect(component['snackBar'].open).toHaveBeenCalledWith(
          'Book borrowed successfully',
          'Close',
          { duration: 3000 }
        );
        done();
      }, 0);
    });

    it('should NOT borrow if user has already borrowed', () => {
      // Arrange
      inventoryServiceSpy.checkBorrowStatus.and.returnValue(of({borrowed : true}));
      component.actionInProgress.set(false);
      inventoryServiceSpy.borrowBook.and.returnValue(throwError(() => new Error('Borrow failed')));

      // Act
      component.borrowBook();

      // Assert
      expect(snackBarSpy.open).toHaveBeenCalledWith(
        'Failed to borrow book',
        'Close',
        { duration: 3000 }
      );
    });
  });

  describe('Return Book Action', () => {
    beforeEach(() => {
      bookServiceSpy.getBookById.and.returnValue(of(mockBook));
      inventoryServiceSpy.checkBorrowStatus.and.returnValue(of({borrowed : true}));
      fixture.detectChanges();
    });

    it('should call inventoryService.returnBook with bookId', () => {
      // Arrange
      const mockBorrowRecord = createMockBorrowRecord({ 
        bookId: 5,
        status: BorrowStatus.RETURNED,
        returnDate: '2025-10-14'
      });
      inventoryServiceSpy.returnBook.and.returnValue(of(mockBorrowRecord));

      // Act
      component.returnBook();

      // Assert
      expect(inventoryServiceSpy.returnBook).toHaveBeenCalledWith(5);
    });

    it('should update book availableCopies after successful return', (done) => {
      // Arrange
      const mockBorrowRecord = createMockBorrowRecord({ 
        bookId: 5,
        status: BorrowStatus.RETURNED,
        returnDate: '2025-10-14'
      });
      inventoryServiceSpy.returnBook.and.returnValue(of(mockBorrowRecord));
      const initialCopies = component.book()!.availableCopies;

      // Act
      component.returnBook();
      TestBed.flushEffects();

      // Assert
      setTimeout(() => {
        expect(component.book()!.availableCopies).toBe(initialCopies + 1);
        done();
      }, 0);
    });

    it('should set hasBorrowed to false after successful return', (done) => {
      // Arrange
      const mockBorrowRecord = createMockBorrowRecord({ 
        bookId: 5,
        status: BorrowStatus.RETURNED,
        returnDate: '2025-10-14'
      });
      inventoryServiceSpy.returnBook.and.returnValue(of(mockBorrowRecord));

      // Act
      component.returnBook();
      TestBed.flushEffects();

      // Assert
      setTimeout(() => {
        expect(component.hasBorrowed()).toBe(false);
        done();
      }, 0);
    });
    
    it('should show success message if return was on time', (done) => {
      // Arrange
      const mockBorrowRecord = createMockBorrowRecord({ 
        bookId: 5,
        status: BorrowStatus.RETURNED,
        returnDate: '2025-10-14',
        isOverdue: false
      });
      inventoryServiceSpy.returnBook.and.returnValue(of(mockBorrowRecord));

      // Act
      component.returnBook();
      TestBed.flushEffects();

      // Assert
      setTimeout(() => {
        expect(component['snackBar'].open).toHaveBeenCalledWith(
          'Book returned successfully',
          'Close',
          { duration: 3000 }
        );
        done();
      }, 0);
    });
  });

  describe('canBorrow getter', () => {
    it('should return false if user has already borrowed', () => {
      // Arrange
      bookServiceSpy.getBookById.and.returnValue(of({...mockBook, availableCopies: 5}));
      inventoryServiceSpy.checkBorrowStatus.and.returnValue(of({borrowed : true}));
      fixture.detectChanges();

      // Act & Assert
      expect(component.canBorrow).toBe(false);
    });

    it('should return true if book available and user has not borrowed', () => {
      // Arrange
      bookServiceSpy.getBookById.and.returnValue(of(mockBook));
      inventoryServiceSpy.checkBorrowStatus.and.returnValue(of({borrowed : false}));
      fixture.detectChanges();

      // Act & Assert
      expect(component.canBorrow).toBe(true);
    });

    it('should return false if no copies available even if user has not borrowed', () => {
      // Arrange
      bookServiceSpy.getBookById.and.returnValue(of({...mockBook, availableCopies: 0}));
      inventoryServiceSpy.checkBorrowStatus.and.returnValue(of({borrowed : false}));
      fixture.detectChanges();

      // Act & Assert
      expect(component.canBorrow).toBe(false);
    });
  });

});
