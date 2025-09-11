package com.librarymanager.backend.dto.response;

import com.librarymanager.backend.entity.BookGenre;

import java.time.LocalDate;

/**
 * Response DTO for book information.
 * Contains only the fields that should be exposed to the frontend/API consumers.
 * Excludes sensitive information like total copies count and internal timestamps.
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
public class BookResponse {
    
    private Long id;
    private String title;
    private String author;
    private String description;
    private BookGenre genre;
    private Integer availableCopies;
    private LocalDate publicationDate;
    
    // Default constructor
    public BookResponse() {}
    
    // Builder pattern constructor
    private BookResponse(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.author = builder.author;
        this.description = builder.description;
        this.genre = builder.genre;
        this.availableCopies = builder.availableCopies;
        this.publicationDate = builder.publicationDate;
    }
    
    // Static builder method
    public static Builder builder() {
        return new Builder();
    }
    
    // Builder class
    public static class Builder {
        private Long id;
        private String title;
        private String author;
        private String description;
        private BookGenre genre;
        private Integer availableCopies;
        private LocalDate publicationDate;
        
        public Builder id(Long id) {
            this.id = id;
            return this;
        }
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public Builder author(String author) {
            this.author = author;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder genre(BookGenre genre) {
            this.genre = genre;
            return this;
        }
        
        public Builder availableCopies(Integer availableCopies) {
            this.availableCopies = availableCopies;
            return this;
        }
        
        public Builder publicationDate(LocalDate publicationDate) {
            this.publicationDate = publicationDate;
            return this;
        }
        
        public BookResponse build() {
            return new BookResponse(this);
        }
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
        return "BookResponse{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", genre=" + genre +
                ", availableCopies=" + availableCopies +
                ", publicationDate=" + publicationDate +
                '}';
    }
}