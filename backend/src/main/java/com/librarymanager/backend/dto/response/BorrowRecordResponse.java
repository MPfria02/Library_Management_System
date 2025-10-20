package com.librarymanager.backend.dto.response;

import com.librarymanager.backend.entity.BorrowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRecordResponse {
    
    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookIsbn;
    private BorrowStatus status;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;  // null if not returned
    private Boolean isOverdue;      // computed field
}