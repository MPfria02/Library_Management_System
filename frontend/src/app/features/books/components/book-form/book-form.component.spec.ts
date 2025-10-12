import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { BookFormComponent } from './book-form.component';
import { AdminBookService } from '../../services/admin-book.service';
import { BookGenre, BookAdminResponse } from '../../models/book.model';
import { MatSnackBar } from '@angular/material/snack-bar';

describe('BookFormComponent', () => {
  let adminSpy: jasmine.SpyObj<AdminBookService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(() => {
    adminSpy = jasmine.createSpyObj('AdminBookService', ['createBook', 'updateBook', 'getBookById']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    TestBed.configureTestingModule({
      imports: [BookFormComponent],
      providers: [
        { provide: AdminBookService, useValue: adminSpy },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map() } } },
        { provide: Router, useValue: routerSpy },
        { provide: MatSnackBar, useValue: snackSpy },
      ]
    });
  });

  it('initializes empty form in create mode', () => {
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    expect(component.isEdit()).toBeFalse();
    expect(component.form.valid).toBeFalse();
  });

  it('loads existing book in edit mode and disables ISBN', () => {
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
    adminSpy.getBookById.and.returnValue(of(book));
    TestBed.overrideProvider(ActivatedRoute, { useValue: { snapshot: { paramMap: new Map([['id', '1']]) } } });
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    expect(component.isEdit()).toBeTrue();
    expect(component.form.get('isbn')?.disabled).toBeTrue();
    expect(component.form.get('availableCopies')?.disabled).toBeTrue();
  });

  it('calls createBook on submit in create mode', () => {
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.form.patchValue({
      isbn: '1234567890',
      title: 'T',
      author: 'A',
      description: '',
      genre: BookGenre.TECHNOLOGY,
      totalCopies: 1,
      availableCopies: 1,
      publicationDate: new Date('2020-01-01')
    });
    adminSpy.createBook.and.returnValue(of({} as BookAdminResponse));
    component.save();
    expect(adminSpy.createBook).toHaveBeenCalled();
  });

  it('calls updateBook on submit in edit mode', () => {
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
    adminSpy.getBookById.and.returnValue(of(book));
    TestBed.overrideProvider(ActivatedRoute, { useValue: { snapshot: { paramMap: new Map([['id', '1']]) } } });

    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    adminSpy.updateBook.and.returnValue(of(book));
    component.form.get('title')?.setValue('Changed');
    component.save();
    expect(adminSpy.updateBook).toHaveBeenCalled();
  });

  it('validates ISBN length (10-20)', () => {
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    const isbn = component.form.get('isbn');
    isbn?.setValue('short');
    expect(isbn?.hasError('minlength')).toBeTrue();
    isbn?.setValue('1234567890');
    expect(isbn?.hasError('minlength')).toBeFalse();
    expect(isbn?.hasError('maxlength')).toBeFalse();
  });

  it('validates title required', () => {
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    const title = component.form.get('title');
    title?.setValue('');
    expect(title?.hasError('required')).toBeTrue();
  });

  it('validates genre required', () => {
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    const genre = component.form.get('genre');
    genre?.setValue(null);
    expect(genre?.hasError('required')).toBeTrue();
  });

  it('validates totalCopies minimum of 1 in create mode', () => {
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    const ctrl = component.form.get('totalCopies');
    ctrl?.setValue(0);
    expect(ctrl?.hasError('min')).toBeTrue();
  });

  it('allows totalCopies of 0 in edit mode', () => {
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
    adminSpy.getBookById.and.returnValue(of(book));
    TestBed.overrideProvider(ActivatedRoute, { useValue: { snapshot: { paramMap: new Map([['id', '1']]) } } });
    
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    
    const ctrl = component.form.get('totalCopies');
    ctrl?.setValue(0);
    expect(ctrl?.hasError('min')).toBeFalse();
  });

  it('validates publicationDate required', () => {
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    const ctrl = component.form.get('publicationDate');
    ctrl?.setValue(null);
    expect(ctrl?.hasError('required')).toBeTrue();
  });

  it('does not submit when form invalid', () => {
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.save();
    expect(adminSpy.createBook).not.toHaveBeenCalled();
    expect(adminSpy.updateBook).not.toHaveBeenCalled();
  });

  it('shows success snackbar and navigates on create success', fakeAsync(() => {
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.form.patchValue({
      isbn: '1234567890',
      title: 'T',
      author: 'A',
      description: '',
      genre: BookGenre.TECHNOLOGY,
      totalCopies: 1,
      availableCopies: 1,
      publicationDate: new Date('2020-01-01')
    });
    adminSpy.createBook.and.returnValue(of({} as BookAdminResponse));
    const snackOpenSpy = spyOn((component as any).snackBar, 'open');
    component.save();
    tick();
    expect(snackOpenSpy).toHaveBeenCalled();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/books']);
  }));

  it('shows error snackbar on failure', fakeAsync(() => {
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.form.patchValue({
      isbn: '1234567890',
      title: 'T',
      author: 'A',
      description: '',
      genre: BookGenre.TECHNOLOGY,
      totalCopies: 1,
      availableCopies: 1,
      publicationDate: new Date('2020-01-01')
    });
    adminSpy.createBook.and.returnValue(throwError(() => new Error('bad')));
    const snackOpenSpy = spyOn((component as any).snackBar, 'open');
    component.save();
    tick();
    expect(snackOpenSpy).toHaveBeenCalled();
  }));

  it('cancels and navigates back on cancel', () => {
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.cancel();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/admin/books']);
  });

  it('auto-adjusts available copies when total copies change in edit mode', () => {
    const book: BookAdminResponse = {
      id: 1,
      isbn: '9780134685991',
      title: 'Effective Java',
      author: 'Joshua Bloch',
      description: 'd',
      genre: BookGenre.TECHNOLOGY,
      totalCopies: 5,
      availableCopies: 3, // 2 borrowed
      publicationDate: '2018-01-01',
      createdAt: '',
      updatedAt: '',
    };
    adminSpy.getBookById.and.returnValue(of(book));
    TestBed.overrideProvider(ActivatedRoute, { useValue: { snapshot: { paramMap: new Map([['id', '1']]) } } });
    
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    
    // Change total copies from 5 to 7 (increase by 2)
    component.form.get('totalCopies')?.setValue(7);
    
    // Available copies should auto-adjust from 3 to 5 (3 + 2)
    expect(component.form.get('availableCopies')?.value).toBe(5);
  });

  it('auto-adjusts available copies when total copies decrease in edit mode', () => {
    const book: BookAdminResponse = {
      id: 1,
      isbn: '9780134685991',
      title: 'Effective Java',
      author: 'Joshua Bloch',
      description: 'd',
      genre: BookGenre.TECHNOLOGY,
      totalCopies: 5,
      availableCopies: 3, // 2 borrowed
      publicationDate: '2018-01-01',
      createdAt: '',
      updatedAt: '',
    };
    adminSpy.getBookById.and.returnValue(of(book));
    TestBed.overrideProvider(ActivatedRoute, { useValue: { snapshot: { paramMap: new Map([['id', '1']]) } } });
    
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    
    // Change total copies from 5 to 3 (decrease by 2)
    component.form.get('totalCopies')?.setValue(3);
    
    // Available copies should auto-adjust from 3 to 1 (3 - 2)
    expect(component.form.get('availableCopies')?.value).toBe(1);
  });

  it('sends correct available copies in edit mode with auto-adjustment', () => {
    const book: BookAdminResponse = {
      id: 1,
      isbn: '9780134685991',
      title: 'Effective Java',
      author: 'Joshua Bloch',
      description: 'd',
      genre: BookGenre.TECHNOLOGY,
      totalCopies: 5,
      availableCopies: 3, // 2 borrowed
      publicationDate: '2018-01-01',
      createdAt: '',
      updatedAt: '',
    };
    adminSpy.getBookById.and.returnValue(of(book));
    adminSpy.updateBook.and.returnValue(of(book));
    TestBed.overrideProvider(ActivatedRoute, { useValue: { snapshot: { paramMap: new Map([['id', '1']]) } } });
    
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    
    // Change total copies from 5 to 7
    component.form.get('totalCopies')?.setValue(7);
    component.form.get('title')?.setValue('Updated Title');
    
    component.save();
    
    // Should send availableCopies as 5 (3 + 2 delta)
    expect(adminSpy.updateBook).toHaveBeenCalledWith(1, jasmine.objectContaining({
      totalCopies: 7,
      availableCopies: 5
    }));
  });

  it('should show form min validator error when totalCopies < borrowed in edit mode', () => {
    const book: BookAdminResponse = {
      id: 1,
      isbn: '9780134685991',
      title: 'Effective Java',
      author: 'Joshua Bloch',
      description: 'd',
      genre: BookGenre.TECHNOLOGY,
      totalCopies: 5,
      availableCopies: 3, // 2 borrowed
      publicationDate: '2018-01-01',
      createdAt: '',
      updatedAt: '',
    };
    adminSpy.getBookById.and.returnValue(of(book));
    TestBed.overrideProvider(ActivatedRoute, { useValue: { snapshot: { paramMap: new Map([['id', '1']]) } } });
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    component.form.patchValue({
      isbn: book.isbn,
      title: book.title, author: book.author, description: book.description, genre: book.genre,
      publicationDate: new Date(book.publicationDate)
    });
    // Set to below borrowed
    component.form.get('totalCopies')?.setValue(1); // Less than borrowed (2)
    fixture.detectChanges();
    expect(component.form.invalid).toBeTrue();
    expect(component.form.get('totalCopies')?.hasError('min')).toBeTrue();
  });

  it('borrowedCopies getter returns correct count in edit mode', () => {
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    component.isEdit.set(true);
    component["originalTotalCopies"] = 5;
    component["originalAvailableCopies"] = 2;
    expect(component.borrowedCopies).toBe(3);
  });

  it('borrowedCopies getter returns 0 in create mode', () => {
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    component.isEdit.set(false);
    component["originalTotalCopies"] = 5;
    component["originalAvailableCopies"] = 2;
    expect(component.borrowedCopies).toBe(0);
  });

  it('should auto-adjust availableCopies to match totalCopies in create mode', () => {
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    // Initially both should be 1
    expect(component.form.get('totalCopies')?.value).toBe(1);
    expect(component.form.get('availableCopies')?.value).toBe(1);

    // Change totalCopies to 5
    component.form.get('totalCopies')?.setValue(5);
    expect(component.form.get('availableCopies')?.value).toBe(5);

    // Change totalCopies to 10
    component.form.get('totalCopies')?.setValue(10);
    expect(component.form.get('availableCopies')?.value).toBe(10);
  });

  it('should set availableCopies equal to totalCopies when saving in create mode', () => {
    const fixture = TestBed.createComponent(BookFormComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.form.patchValue({
      isbn: '1234567890',
      title: 'Test Book',
      author: 'Test Author',
      description: '',
      genre: BookGenre.TECHNOLOGY,
      totalCopies: 5,
      publicationDate: new Date('2020-01-01')
    });

    adminSpy.createBook.and.returnValue(of({} as BookAdminResponse));
    component.save();

    expect(adminSpy.createBook).toHaveBeenCalledWith(jasmine.objectContaining({
      totalCopies: 5,
      availableCopies: 5  // Should equal totalCopies in create mode
    }));
  });
});


