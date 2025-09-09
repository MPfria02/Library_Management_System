package com.librarymanager.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.time.LocalDate;

@Entity
@Table(name = "books")
public class Book {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 20)
    @NotBlank(message = "ISBN is required")
    @Size(max = 20, message = "ISBN cannot exceed 20 characters")
    private String isbn;
    
    @Column(nullable = false, length = 200)
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;
    
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Author is required")
    @Size(max = 100, message = "Author cannot exceed 100 characters")
    private String author;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Genre is required")
    private BookGenre genre;
    
    @Column(name = "total_copies", nullable = false)
    @NotNull(message = "Total copies is required")
    @Min(value = 1, message = "Total copies must be at least 1")
    private Integer totalCopies = 1;
    
    @Column(name = "available_copies", nullable = false)
    @NotNull(message = "Available copies is required")
    @Min(value = 0, message = "Available copies cannot be negative")
    private Integer availableCopies = 1;
    
    @Column(name = "publication_date")
    private LocalDate publicationDate;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;
    
    // Constructors
    public Book() {}
    
    // Builder pattern constructor
    private Book(Builder builder) {
        this.isbn = builder.isbn;
        this.title = builder.title;
        this.author = builder.author;
        this.description = builder.description;
        this.genre = builder.genre;
        this.totalCopies = builder.totalCopies != null ? builder.totalCopies : 1;
        this.availableCopies = builder.availableCopies != null ? builder.availableCopies : this.totalCopies;
        this.publicationDate = builder.publicationDate;
    }
    
    // Static builder method
    public static Builder builder() {
        return new Builder();
    }
    
    // Business Logic Methods (Domain-Driven Design)
    public boolean isAvailable() {
        return availableCopies != null && availableCopies > 0;
    }
    
    public void borrowCopy() {
        if (!isAvailable()) {
            throw new IllegalStateException("Book '" + this.title + "' is not available for borrowing");
        }
        this.availableCopies--;
    }
    
    public void returnCopy() {
        if (this.availableCopies >= this.totalCopies) {
            throw new IllegalStateException("Cannot return more copies than total available");
        }
        this.availableCopies++;
    }
    
    // Builder class
    public static class Builder {
        private String isbn;
        private String title;
        private String author;
        private String description;
        private BookGenre genre;
        private Integer totalCopies;
        private Integer availableCopies;
        private LocalDate publicationDate;
        
        public Builder isbn(String isbn) {
            this.isbn = isbn;
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
        
        public Builder totalCopies(Integer totalCopies) {
            this.totalCopies = totalCopies;
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
        
        public Book build() {
            return new Book(this);
        }
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}