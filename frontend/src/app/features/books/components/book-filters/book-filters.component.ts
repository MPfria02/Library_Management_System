import { CommonModule } from '@angular/common';
import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { BookGenre, BookSearchFilters } from '../../models/book.model';

@Component({
  selector: 'app-book-filters',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatButtonModule
  ],
  templateUrl: './book-filters.component.html',
  styleUrls: ['./book-filters.component.scss']
})
export class BookFiltersComponent {
  filterForm: FormGroup;

  // Include null for "All Genres"
  readonly genreOptions: Array<{ label: string; value: BookGenre | null }> = [
    { label: 'All Genres', value: null },
    { label: 'Fiction', value: BookGenre.FICTION },
    { label: 'Technology', value: BookGenre.TECHNOLOGY },
    { label: 'Science', value: BookGenre.SCIENCE },
    { label: 'History', value: BookGenre.HISTORY },
    { label: 'Biography', value: BookGenre.BIOGRAPHY },
    { label: 'Fantasy', value: BookGenre.FANTASY },
    { label: 'Mystery', value: BookGenre.MYSTERY }
  ];

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<BookFiltersComponent>,
    @Inject(MAT_DIALOG_DATA) public data: BookSearchFilters
  ) {
    this.filterForm = this.fb.group({
      searchTerm: [data?.searchTerm ?? ''],
      genre: [data?.genre ?? null],
      availableOnly: [data?.availableOnly ?? false]
    });
  }

  applyFilters(): void {
    const raw = this.filterForm.value as {
      searchTerm: string;
      genre: BookGenre | null;
      availableOnly: boolean;
    };

    const filters: BookSearchFilters = {
      searchTerm: raw.searchTerm && raw.searchTerm.trim() !== '' ? raw.searchTerm.trim() : undefined,
      genre: raw.genre ?? undefined,
      availableOnly: raw.availableOnly ? true : undefined
    };

    this.dialogRef.close(filters);
  }

  clearFilters(): void {
    const defaults = { searchTerm: '', genre: null, availableOnly: false };
    this.filterForm.reset(defaults);
  }

  cancel(): void {
    this.dialogRef.close();
  }
}


