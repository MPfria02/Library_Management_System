// src/app/features/borrows/components/borrow-card/borrow-card.component.ts

import { Component, Input, Output, EventEmitter, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import {MatChipsModule} from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { BorrowRecordResponse, BorrowStatus } from '../../models/borrow-record.model';

@Component({
  selector: 'app-borrow-card',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatChipsModule,
    MatIconModule
  ],
  templateUrl: './borrow-card.component.html',
  styleUrl: './borrow-card.component.scss'
})
export class BorrowCardComponent {
  @Input({ required: true }) borrowRecord!: BorrowRecordResponse;
  @Output() viewDetails = new EventEmitter<number>();

  // Expose enum to template
  BorrowStatus = BorrowStatus;

  onViewDetails(): void {
    this.viewDetails.emit(this.borrowRecord.bookId);
  }
}