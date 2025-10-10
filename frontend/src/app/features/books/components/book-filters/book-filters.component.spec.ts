import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { BookFiltersComponent } from './book-filters.component';
import { BookGenre, BookSearchFilters } from '../../models/book.model';
import { By } from '@angular/platform-browser';

describe('BookFiltersComponent', () => {
  let component: BookFiltersComponent;
  let fixture: ComponentFixture<BookFiltersComponent>;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<BookFiltersComponent>>;

  const injectedData: BookSearchFilters = {
    searchTerm: 'Java',
    genre: BookGenre.TECHNOLOGY,
    availableOnly: true
  };

  beforeEach(async () => {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      imports: [BookFiltersComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: injectedData }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BookFiltersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with injected data', () => {
    expect(component.filterForm.value).toEqual({
      searchTerm: 'Java',
      genre: BookGenre.TECHNOLOGY,
      availableOnly: true
    });
  });

  it('should populate genre dropdown including All Genres', () => {
    const options = component.genreOptions.map(o => o.label);
    expect(options).toContain('All Genres');
    expect(options).toContain('Technology');
    expect(options.length).toBeGreaterThan(1);
  });

  it('should apply filters with values', () => {
    component.filterForm.setValue({ searchTerm: 'Clean', genre: BookGenre.TECHNOLOGY, availableOnly: false });
    component.applyFilters();
    expect(dialogRefSpy.close).toHaveBeenCalledWith({
      searchTerm: 'Clean',
      genre: BookGenre.TECHNOLOGY,
      availableOnly: undefined
    });
  });

  it('should convert empty string and null to undefined on apply', () => {
    component.filterForm.setValue({ searchTerm: '', genre: null, availableOnly: false });
    component.applyFilters();
    expect(dialogRefSpy.close).toHaveBeenCalledWith({
      searchTerm: undefined,
      genre: undefined,
      availableOnly: undefined
    });
  });

  it('should reset form to defaults when clear is clicked', () => {
    component.filterForm.setValue({
      searchTerm: 'Test',
      genre: BookGenre.FICTION,
      availableOnly: true
    });
  
    component.clearFilters();
  
    expect(component.filterForm.value.searchTerm).toBe('');
    expect(component.filterForm.value.genre).toBeNull();
    expect(component.filterForm.value.availableOnly).toBe(false);
  });

  it('should close without data on cancel', () => {
    component.cancel();
    expect(dialogRefSpy.close).toHaveBeenCalledWith();
  });

  it('should toggle availableOnly checkbox', () => {
    const initialValue = component.filterForm.value.availableOnly;
    
    component.filterForm.patchValue({ availableOnly: !initialValue });
    fixture.detectChanges();
  
    expect(component.filterForm.value.availableOnly).toBe(!initialValue);
  });
});


