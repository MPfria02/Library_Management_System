import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { BookManagementComponent } from './book-management.component';
import { BookAdminResponse, BookSearchFilters, PageResponse, BookGenre } from '../../models/book.model';
import { AdminBookService } from '../../services/admin-book.service';

describe('BookManagementComponent', () => {
  let adminServiceSpy: jasmine.SpyObj<AdminBookService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;

  const page = (items: BookAdminResponse[]): PageResponse<BookAdminResponse> => ({
    content: items,
    totalPages: 1,
    totalElements: items.length,
    size: 30,
    number: 0,
    first: true,
    last: true,
    empty: items.length === 0
  });

  const book: BookAdminResponse = {
    id: 1,
    isbn: '9780134685991',
    title: 'Effective Java',
    author: 'Joshua Bloch',
    description: 'd',
    genre: BookGenre.TECHNOLOGY,
    totalCopies: 5,
    availableCopies: 5,
    publicationDate: '2018-01-01',
    createdAt: '',
    updatedAt: '',
  };

  beforeEach(() => {
    adminServiceSpy = jasmine.createSpyObj('AdminBookService', ['getBooks', 'deleteBook']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    TestBed.configureTestingModule({
      imports: [BookManagementComponent],
      providers: [
        { provide: AdminBookService, useValue: adminServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: MatDialog, useValue: jasmine.createSpyObj('MatDialog', ['open']) },
      ],
    });
    dialogSpy = TestBed.inject(MatDialog) as jasmine.SpyObj<MatDialog>;
  });

  function create(): BookManagementComponent {
    const fixture = TestBed.createComponent(BookManagementComponent);
    const component = fixture.componentInstance;
    // Force inject our dialog spy into the component instance
    (component as any).dialog = dialogSpy;
    (component as any).snackBar = snackSpy;
    return component;
  }

  it('loads books on init', () => {
    adminServiceSpy.getBooks.and.returnValue(of(page([book])));
    const component = create();
    component.ngOnInit();
    expect(adminServiceSpy.getBooks).toHaveBeenCalled();
    expect(component.books().length).toBe(1);
  });

  it('displays loading spinner while fetching', () => {
    adminServiceSpy.getBooks.and.returnValue(of(page([book])));
    const component = create();
    component.loading.set(true);
    expect(component.loading()).toBeTrue();
  });

  it('shows empty state when no books', () => {
    adminServiceSpy.getBooks.and.returnValue(of(page([])));
    const component = create();
    component.ngOnInit();
    expect(component.books().length).toBe(0);
  });

  it('navigates to create form on Add New Book click', () => {
    adminServiceSpy.getBooks.and.returnValue(of(page([])));
    const component = create();
    component.addNewBook();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/books/new']);
  });

  it('opens filter dialog on Search/Filter click', () => {
    adminServiceSpy.getBooks.and.returnValue(of(page([book])));
    dialogSpy.open.and.returnValue({
      afterClosed: () => of(undefined)
    } as any);
    const component = create();
    component.ngOnInit();
    component.openFilterDialog();
    expect(dialogSpy.open).toHaveBeenCalled();
  });

  it('applies filters and reloads books on dialog close', () => {
    const filters: BookSearchFilters = { searchTerm: 'Java' };
    dialogSpy.open.and.returnValue({ afterClosed: () => of(filters) } as any);
    adminServiceSpy.getBooks.and.returnValues(of(page([book])), of(page([book])));
    const component = create();
    component.ngOnInit();
    component.openFilterDialog();
    expect(adminServiceSpy.getBooks).toHaveBeenCalledTimes(2);
  });

  it('navigates to edit form on edit button click', () => {
    const component = create();
    component.editBook(1);
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/books', 1, 'edit']);
  });

  it('opens delete dialog on delete button click', () => {
    dialogSpy.open.and.returnValue({ afterClosed: () => of(null) } as any);
    const component = create();
    component.deleteBook(book);
    expect(dialogSpy.open).toHaveBeenCalled();
  });

  it('reloads books after successful delete', () => {
    dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as any);
    adminServiceSpy.deleteBook.and.returnValue(of(void 0));
    adminServiceSpy.getBooks.and.returnValues(of(page([book])), of(page([])));
    const component = create();
    component.ngOnInit();
    component.deleteBook(book);
    expect(adminServiceSpy.deleteBook).toHaveBeenCalledWith(1);
    expect(adminServiceSpy.getBooks).toHaveBeenCalledTimes(2);
  });

  it('handles pagination changes', () => {
    adminServiceSpy.getBooks.and.returnValues(of(page([book])), of(page([book])));
    const component = create();
    component.ngOnInit();
    component.onPageChange({ pageIndex: 1 } as any);
    expect(component.currentPage()).toBe(1);
    expect(adminServiceSpy.getBooks).toHaveBeenCalledTimes(2);
  });

  it('shows error snackbar on API failure', fakeAsync(() => {
    adminServiceSpy.getBooks.and.returnValue(throwError(() => new Error('bad')));
    const component = create();
    component.loadBooks();
    tick();
    expect(snackSpy.open).toHaveBeenCalled();
  }));
});


