package com.librarymanager.backend.dto.request;

import com.librarymanager.backend.entity.BookGenre;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * Request DTO for creating new books in the system.
 * Contains all necessary fields for book creation with comprehensive validation.
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
public class BookCreationRequest {
    
    @NotBlank(message = "ISBN is required")
    @Size(min = 10, max = 20, message = "ISBN must be between 10 and 20 characters")
    private String isbn;
    
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;
    
    @NotBlank(message = "Author is required")
    @Size(max = 100, message = "Author cannot exceed 100 characters")
    private String author;
    
    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
    
    @NotNull(message = "Genre is required")
    private BookGenre genre;
    
    @NotNull(message = "Total copies is required")
    @Min(value = 1, message = "Total copies must be at least 1")
    private Integer totalCopies = 1;

    private Integer availableCopies;

    @NotNull(message = "Publication date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate publicationDate;
    
    // Constructors
    public BookCreationRequest() {}
    
    public BookCreationRequest(String isbn, String title, String author, String description, 
                              BookGenre genre, Integer totalCopies, LocalDate publicationDate) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.description = description;
        this.genre = genre;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies; // Initially, all copies are available
        this.publicationDate = publicationDate;
    }
    
    // Getters and Setters
    public String getIsbn() {
        return isbn;
    }
    
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BookGenre getGenre() {
        return genre;
    }
    
    public void setGenre(BookGenre genre) {
        this.genre = genre;
    }
    
    public Integer getTotalCopies() {
        return totalCopies;
    }
    
    public void setTotalCopies(Integer totalCopies) {
        this.totalCopies = totalCopies;
    }
    
    public Integer getAvailableCopies() {
        return availableCopies;
    }
    
    public void setAvailableCopies(Integer availableCopies) {
        this.availableCopies = availableCopies;
    }
    
    public LocalDate getPublicationDate() {
        return publicationDate;
    }
    
    public void setPublicationDate(LocalDate publicationDate) {
        this.publicationDate = publicationDate;
    }
    
    @Override
    public String toString() {
        return "BookCreationRequest{" +
                "isbn='" + isbn + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", genre=" + genre +
                ", totalCopies=" + totalCopies +
                ", availableCopies=" + availableCopies +
                ", publicationDate=" + publicationDate +
                '}';
    }
}