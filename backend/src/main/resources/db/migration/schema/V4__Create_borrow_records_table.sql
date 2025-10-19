-- Migration: Create borrow records table
-- Description: Creates the borrow records table with proper constraints, indexes, and PostgreSQL-specific features
-- Author: Marcel Pulido
-- Date: 2025

CREATE TABLE borrow_records (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    borrow_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
       -- Constraints
    CONSTRAINT fk_borrow_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_borrow_book FOREIGN KEY (book_id) 
        REFERENCES books(id) ON DELETE CASCADE,
    CONSTRAINT chk_status CHECK (status IN ('BORROWED', 'RETURNED')),
    CONSTRAINT chk_dates CHECK (due_date >= borrow_date),
    CONSTRAINT chk_return_date CHECK (return_date IS NULL OR return_date >= borrow_date)
);

-- Indexes for query performance
CREATE INDEX idx_borrow_user_id ON borrow_records(user_id);
CREATE INDEX idx_borrow_book_id ON borrow_records(book_id);
CREATE INDEX idx_borrow_status ON borrow_records(status);
CREATE INDEX idx_borrow_user_status ON borrow_records(user_id, status);
CREATE INDEX idx_borrow_due_date ON borrow_records(due_date);

-- Composite index for most common query: "user's active borrows sorted by due date"
CREATE INDEX idx_borrow_user_status_due ON borrow_records(user_id, status, due_date);

-- Unique constraint: User can only have ONE active borrow of the same book
CREATE UNIQUE INDEX idx_unique_active_borrow 
    ON borrow_records(user_id, book_id) 
    WHERE status = 'BORROWED';

-- Comments for documentation
COMMENT ON TABLE borrow_records IS 'Tracks all book borrow transactions with full history';
COMMENT ON COLUMN borrow_records.borrow_date IS 'Date the book was borrowed';
COMMENT ON COLUMN borrow_records.due_date IS 'Date the book is due (calculated as borrow_date + 7 days)';
COMMENT ON COLUMN borrow_records.return_date IS 'Date the book was returned (NULL if still borrowed)';
COMMENT ON COLUMN borrow_records.status IS 'Current status: BORROWED or RETURNED';