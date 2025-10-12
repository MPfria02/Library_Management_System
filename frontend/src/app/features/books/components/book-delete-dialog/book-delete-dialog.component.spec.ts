import { TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { BookAdminResponse, BookGenre, BookResponse } from '../../models/book.model';
import { BookDeleteDialogComponent } from './book-delete-dialog.component';

describe('BookDeleteDialogComponent', () => {
  let component: BookDeleteDialogComponent;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<BookDeleteDialogComponent>>;

  const book: BookAdminResponse = {
    id: 1,
    isbn: '9780134685991',
    title: 'Effective Java',
    author: 'Joshua Bloch',
    description: 'desc',
    genre: BookGenre.TECHNOLOGY,
    totalCopies: 5,
    availableCopies: 5,
    publicationDate: '2018-01-01',
    createdAt: '',
    updatedAt: ''
  };

  beforeEach(() => {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);
    TestBed.configureTestingModule({
      imports: [BookDeleteDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: book }
      ]
    });
    const fixture = TestBed.createComponent(BookDeleteDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('displays book data (title/author)', () => {
    expect(component.data.title).toContain('Effective Java');
    expect(component.data.author).toContain('Joshua');
  });

  it('closes with null on cancel', () => {
    component.cancel();
    expect(dialogRefSpy.close).toHaveBeenCalledWith(null);
  });

  it('closes with true on confirm', () => {
    component.confirm();
    expect(dialogRefSpy.close).toHaveBeenCalledWith(true);
  });
});


