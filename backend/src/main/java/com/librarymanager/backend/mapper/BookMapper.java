package com.librarymanager.backend.mapper;

import com.librarymanager.backend.dto.request.BookCreationRequest;
import com.librarymanager.backend.dto.response.BookResponse;
import com.librarymanager.backend.dto.response.BookAdminResponse;
import com.librarymanager.backend.entity.Book;
import org.springframework.stereotype.Component;

/**
 * Mapper class for converting between Book entities and DTOs.
 * 
 * This class provides mapping methods to convert Book entities to response DTOs
 * and creation requests to Book entities. It handles the business logic for
 * setting default values and ensures consistent mapping across the application.
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
@Component
public class BookMapper {
    
    /**
     * Converts a BookCreationRequest to a Book entity.
     * 
     * Applies business rules:
     * - If availableCopies is null, sets it equal to totalCopies
     * - Ensures all required fields are properly mapped
     * 
     * @param request the creation request containing book data
     * @return Book entity ready for persistence
     * @throws IllegalArgumentException if request is null
     */
    public Book toEntity(BookCreationRequest request) {    
        return Book.builder()
                .isbn(request.getIsbn())
                .title(request.getTitle())
                .author(request.getAuthor())
                .description(request.getDescription())
                .genre(request.getGenre())
                .totalCopies(request.getTotalCopies())
                .availableCopies(request.getAvailableCopies())
                .publicationDate(request.getPublicationDate())
                .build();
    }
    
    /**
     * Converts a Book entity to a BookResponse DTO.
     * Only includes fields that should be exposed to API consumers.
     * Excludes sensitive information like total copies and internal timestamps.
     * 
     * @param book the Book entity to convert
     * @return BookResponse DTO for API responses
     * @throws IllegalArgumentException if book is null
     */
    public BookResponse toResponse(Book book) {
        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .description(book.getDescription())
                .genre(book.getGenre())
                .availableCopies(book.getAvailableCopies())
                .publicationDate(book.getPublicationDate())
                .build();
    }

    /**
     * Converts a Book entity to BookAdminResponse DTO with all fields.
     * Used for admin endpoints that require full book data including ISBN and total copies.
     *
     * @param book the book entity to convert
     * @return BookAdminResponse with all book fields populated
     */
    public BookAdminResponse toAdminResponse(Book book) {
        if (book == null) return null;
        return BookAdminResponse.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .description(book.getDescription())
                .genre(book.getGenre())
                .totalCopies(book.getTotalCopies())
                .availableCopies(book.getAvailableCopies())
                .publicationDate(book.getPublicationDate())
                .createdAt(book.getCreatedAt() != null ? book.getCreatedAt().toLocalDateTime() : null)
                .updatedAt(book.getUpdatedAt() != null ? book.getUpdatedAt().toLocalDateTime() : null)
                .build();
    }
}