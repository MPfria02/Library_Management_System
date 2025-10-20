package com.librarymanager.backend.mapper;

import com.librarymanager.backend.dto.response.BorrowRecordResponse;
import com.librarymanager.backend.entity.BorrowRecord;
import com.librarymanager.backend.entity.BorrowStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class BorrowRecordMapper {
    
    /**
     * Convert BorrowRecord entity to response DTO
     * Includes computed isOverdue field
     */
    public BorrowRecordResponse toResponse(BorrowRecord borrowRecord) {
        if (borrowRecord == null) {
            return null;
        }
        
        return BorrowRecordResponse.builder()
            .id(borrowRecord.getId())
            .bookId(borrowRecord.getBook().getId())
            .bookTitle(borrowRecord.getBook().getTitle())
            .bookAuthor(borrowRecord.getBook().getAuthor())
            .bookIsbn(borrowRecord.getBook().getIsbn())
            .status(borrowRecord.getStatus())
            .borrowDate(borrowRecord.getBorrowDate())
            .dueDate(borrowRecord.getDueDate())
            .returnDate(borrowRecord.getReturnDate())
            .isOverdue(computeIsOverdue(borrowRecord))
            .build();
    }
    
    /**
     * Compute overdue status:
     * - Must be BORROWED (not returned)
     * - Due date must be in the past
     */
    private Boolean computeIsOverdue(BorrowRecord borrowRecord) {
        if (borrowRecord.getStatus() != BorrowStatus.BORROWED) {
            return false;
        }
        return LocalDate.now().isAfter(borrowRecord.getDueDate());
    }
}