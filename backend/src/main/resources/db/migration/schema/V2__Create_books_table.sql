-- Migration: Create books table
-- Description: Creates the books table with full-text search capabilities and proper constraints
-- Author: Marcel Pulido
-- Date: 2025

-- Create books table
CREATE TABLE books (
    id BIGSERIAL PRIMARY KEY,
    isbn VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    author VARCHAR(100) NOT NULL,
    description TEXT,
    genre VARCHAR(50) NOT NULL,
    total_copies INTEGER NOT NULL DEFAULT 1,
    available_copies INTEGER NOT NULL DEFAULT 1,
    publication_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT books_isbn_check CHECK (LENGTH(isbn) >= 10 AND LENGTH(isbn) <= 20),
    CONSTRAINT books_title_check CHECK (LENGTH(title) > 0),
    CONSTRAINT books_author_check CHECK (LENGTH(author) > 0),
    CONSTRAINT books_genre_check CHECK (genre IN (
        'FICTION', 'NON_FICTION', 'SCIENCE', 'TECHNOLOGY', 
        'HISTORY', 'BIOGRAPHY', 'MYSTERY', 'ROMANCE', 'FANTASY'
    )),
    CONSTRAINT books_total_copies_check CHECK (total_copies >= 1),
    CONSTRAINT books_available_copies_check CHECK (available_copies >= 0),
    CONSTRAINT books_copies_logic_check CHECK (available_copies <= total_copies),
    CONSTRAINT books_publication_date_check CHECK (publication_date <= CURRENT_DATE)
);

-- Create indexes for performance
CREATE INDEX idx_books_isbn ON books(isbn);
CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_books_author ON books(author);
CREATE INDEX idx_books_genre ON books(genre);
CREATE INDEX idx_books_available_copies ON books(available_copies);
CREATE INDEX idx_books_created_at ON books(created_at);

-- PostgreSQL Full-Text Search indexes (matching your BookRepository)
CREATE INDEX idx_books_title_fts ON books USING GIN (to_tsvector('english', title));
CREATE INDEX idx_books_author_fts ON books USING GIN (to_tsvector('english', author));
CREATE INDEX idx_books_title_author_fts ON books USING GIN (to_tsvector('english', title || ' ' || author));

-- Combined functional index for common searches
CREATE INDEX idx_books_title_author_lower ON books(LOWER(title), LOWER(author));
CREATE INDEX idx_books_available_genre ON books(genre, available_copies) WHERE available_copies > 0;

-- Create updated_at trigger for books
CREATE TRIGGER update_books_updated_at 
    BEFORE UPDATE ON books 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE books IS 'Book catalog for library management system';
COMMENT ON COLUMN books.id IS 'Primary key - auto-generated book ID';
COMMENT ON COLUMN books.isbn IS 'International Standard Book Number (unique)';
COMMENT ON COLUMN books.title IS 'Book title (required)';
COMMENT ON COLUMN books.author IS 'Book author (required)';
COMMENT ON COLUMN books.description IS 'Optional book description';
COMMENT ON COLUMN books.genre IS 'Book genre from predefined categories';
COMMENT ON COLUMN books.total_copies IS 'Total number of copies owned';
COMMENT ON COLUMN books.available_copies IS 'Number of copies currently available';
COMMENT ON COLUMN books.publication_date IS 'Date the book was published';
COMMENT ON COLUMN books.created_at IS 'Timestamp when book was added to system';
COMMENT ON COLUMN books.updated_at IS 'Timestamp when book was last updated';