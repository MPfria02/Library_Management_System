import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AdminBookService } from '../../services/admin-book.service';
import { BookCreationRequest, BookGenre, BookAdminResponse } from '../../models/book.model';

@Component({
  selector: 'app-book-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatSnackBarModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  templateUrl: './book-form.component.html',
  styleUrls: ['./book-form.component.scss']
})
export class BookFormComponent implements OnInit {
  form!: FormGroup;
  isEdit = signal<boolean>(false);
  bookTitle = signal<string>('');
  readonly genres = Object.values(BookGenre);
  submitting = signal<boolean>(false);
  
  // Store original values for auto-adjustment in edit mode
  private originalTotalCopies = 0;
  private originalAvailableCopies = 0;
  
  // Today's date for datepicker max date
  today = new Date();

  private fb = inject(FormBuilder);
  private adminService = inject(AdminBookService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  get borrowedCopies(): number {
    if (!this.isEdit()) return 0;
    return this.originalTotalCopies - this.originalAvailableCopies;
  }

  ngOnInit(): void {
    this.form = this.fb.group({
      isbn: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(20)]],
      title: ['', [Validators.required, Validators.maxLength(200)]],
      author: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(2000)]],
      genre: [null, [Validators.required]],
      totalCopies: [1, [Validators.required, Validators.min(1)]],
      availableCopies: [1, [Validators.required, Validators.min(0)]],
      publicationDate: [null, [Validators.required]]
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEdit.set(true);
      const id = Number(idParam);
      this.form.get('isbn')?.disable();
      this.form.get('availableCopies')?.disable();
      this.adminService.getBookById(id).subscribe({
        next: (book: BookAdminResponse) => {
          this.bookTitle.set(book.title);

          // Store original values for auto-adjustment
          this.originalTotalCopies = book.totalCopies;
          this.originalAvailableCopies = book.availableCopies;

          this.form.patchValue({
            isbn: book.isbn,
            title: book.title,
            author: book.author,
            description: book.description,
            genre: book.genre,
            totalCopies: book.totalCopies,
            availableCopies: book.availableCopies,
            publicationDate: new Date(book.publicationDate)
          });

          // Set up dynamic min validator for totalCopies (min = borrowed copies)
          const borrowed = this.originalTotalCopies - this.originalAvailableCopies;
          this.form.get('totalCopies')?.setValidators([
            Validators.required,
            Validators.min(borrowed)
          ]);
          this.form.get('totalCopies')?.updateValueAndValidity();

          // Set up auto-adjustment for edit mode
          this.setupEditModeAutoAdjustment();
        },
        error: (_err: unknown) => {
          this.snackBar.open('Failed to load book', 'Close', { duration: 3000 });
        }
      });
    } else {
      this.isEdit.set(false);
      // In create mode: totalCopies min 1, availableCopies mirrors totalCopies, cannot be manually set
      this.form.get('availableCopies')?.disable();
      this.form.get('totalCopies')?.setValidators([
        Validators.required,
        Validators.min(1)
      ]);
      this.form.get('totalCopies')?.updateValueAndValidity();
      
      // Set up auto-adjustment for create mode - availableCopies always equals totalCopies
      this.setupCreateModeAutoAdjustment();
    }
  }

  private setupCreateModeAutoAdjustment(): void {
    // In create mode, availableCopies always equals totalCopies
    this.form.get('totalCopies')?.valueChanges.subscribe((newTotalCopies: number) => {
      if (!this.isEdit()) {
        // Update availableCopies to match totalCopies
        this.form.get('availableCopies')?.setValue(newTotalCopies, { emitEvent: false });
      }
    });
  }

  private setupEditModeAutoAdjustment(): void {
    this.form.get('totalCopies')?.valueChanges.subscribe((newTotalCopies: number) => {
      if (this.isEdit()) {
        const delta = newTotalCopies - this.originalTotalCopies;
        const newAvailableCopies = this.originalAvailableCopies + delta;
        
        // Update the available copies display (but don't send to backend)
        this.form.get('availableCopies')?.setValue(newAvailableCopies, { emitEvent: false });
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/books']);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    const raw = this.form.getRawValue();
    
    // Calculate auto-adjusted available copies
    let availableCopies = raw.availableCopies;
    if (this.isEdit()) {
      // In edit mode: adjust based on delta from original
      const delta = raw.totalCopies - this.originalTotalCopies;
      availableCopies = this.originalAvailableCopies + delta;
    } else {
      // In create mode: availableCopies always equals totalCopies
      availableCopies = raw.totalCopies;
    }

    const request: BookCreationRequest = {
      isbn: raw.isbn,
      title: raw.title,
      author: raw.author,
      description: raw.description ?? '',
      genre: raw.genre,
      totalCopies: raw.totalCopies,
      availableCopies: availableCopies,
      publicationDate: this.toIsoDate(raw.publicationDate)
    };

    if (this.isEdit()) {
      const id = Number(this.route.snapshot.paramMap.get('id'));
      this.adminService.updateBook(id, request).subscribe({
        next: () => this.onSuccess('Book updated successfully'),
        error: (err: unknown) => this.onError(err)
      });
    } else {
      this.adminService.createBook(request).subscribe({
        next: () => this.onSuccess('Book created successfully'),
        error: (err: unknown) => this.onError(err)
      });
    }
  }

  private toIsoDate(date: Date): string {
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private onSuccess(message: string): void {
    this.submitting.set(false);
    this.snackBar.open(message, 'Close', { duration: 2500 });
    this.router.navigate(['/admin/books']);
  }

  private onError(err: unknown): void {
    console.error(err);
    this.submitting.set(false);
    this.snackBar.open('Failed to save book', 'Close', { duration: 3000 });
  }
}


