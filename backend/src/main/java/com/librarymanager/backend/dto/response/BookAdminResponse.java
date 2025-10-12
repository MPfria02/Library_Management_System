package com.librarymanager.backend.dto.response;

import com.librarymanager.backend.entity.BookGenre;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Admin-only Data Transfer Object containing ALL book fields.
 * Used for admin API responses to present full book data, including fields hidden from members (isbn, totalCopies).
 *
 * Fields:
 *  - id
 *  - isbn
 *  - title
 *  - author
 *  - description
 *  - genre
 *  - totalCopies
 *  - availableCopies
 *  - publicationDate
 *  - createdAt
 *  - updatedAt
 *
 * @author Marcel Pulido
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookAdminResponse {
    private Long id;
    private String isbn;
    private String title;
    private String author;
    private String description;
    private BookGenre genre;
    private Integer totalCopies;
    private Integer availableCopies;
    private LocalDate publicationDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
