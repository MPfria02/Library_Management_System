import { CommonModule } from '@angular/common';
import { Component, Inject } from '@angular/core';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { BookAdminResponse, BookResponse } from '../../models/book.model';

@Component({
  selector: 'app-book-delete-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './book-delete-dialog.component.html',
  styleUrls: ['./book-delete-dialog.component.scss']
})
export class BookDeleteDialogComponent {
  constructor(
    private readonly dialogRef: MatDialogRef<BookDeleteDialogComponent, boolean | null>,
    @Inject(MAT_DIALOG_DATA) public data: BookAdminResponse
  ) {}

  cancel(): void {
    this.dialogRef.close(null);
  }

  confirm(): void {
    this.dialogRef.close(true);
  }
}


