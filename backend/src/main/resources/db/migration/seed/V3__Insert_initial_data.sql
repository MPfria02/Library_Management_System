-- Migration: Insert initial test data
-- Description: Inserts sample users and books for testing and demonstration
-- Author: Marcel Pulido
-- Date: 2025
-- NOTE: This includes password hashes for development only - replace in production

-- Insert initial admin user
-- Password: "admin123" (BCrypt encoded)
-- In production, this should be created through proper registration flow
INSERT INTO users (email, password, first_name, last_name, role) VALUES 
('admin@library.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LeP4.VbQi7JGF8Baa', 'Library', 'Administrator', 'ADMIN');

-- Insert sample members
-- Password: "member123" (BCrypt encoded) 
INSERT INTO users (email, password, first_name, last_name, phone, role) VALUES 
('marcel.pulido@email.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LeP4.VbQi7JGF8Baa', 'Marcel', 'Pulido', '555-0123', 'MEMBER'),
('john.doe@email.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LeP4.VbQi7JGF8Baa', 'John', 'Doe', '555-0124', 'MEMBER'),
('jane.smith@email.com', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LeP4.VbQi7JGF8Baa', 'Jane', 'Smith', '555-0125', 'MEMBER');

-- Insert sample books
INSERT INTO books (isbn, title, author, description, genre, total_copies, available_copies, publication_date) VALUES 
-- Technology Books
('9780134685991', 'Effective Java', 'Joshua Bloch', 'Best practices for the Java programming language', 'TECHNOLOGY', 3, 3, '2017-12-27'),
('9781617294945', 'Spring Boot in Action', 'Craig Walls', 'Comprehensive guide to Spring Boot framework', 'TECHNOLOGY', 2, 2, '2015-12-16'),
('9780135166307', 'Clean Code', 'Robert C. Martin', 'A handbook of agile software craftsmanship', 'TECHNOLOGY', 4, 3, '2008-08-01'),

-- Fiction Books  
('9780544003415', 'The Lord of the Rings', 'J.R.R. Tolkien', 'Epic fantasy adventure in Middle-earth', 'FANTASY', 5, 4, '1954-07-29'),
('9780061120084', 'To Kill a Mockingbird', 'Harper Lee', 'Classic American literature', 'FICTION', 3, 3, '1960-07-11'),
('9780307387899', 'The Road', 'Cormac McCarthy', 'Post-apocalyptic father-son journey', 'FICTION', 2, 1, '2006-09-26'),

-- Science Books
('9781250078100', 'Sapiens', 'Yuval Noah Harari', 'A brief history of humankind', 'SCIENCE', 3, 2, '2014-02-10'),
('9780553380163', 'A Brief History of Time', 'Stephen Hawking', 'From the Big Bang to Black Holes', 'SCIENCE', 2, 2, '1988-04-01'),

-- History Books
('9781476728810', 'Guns, Germs, and Steel', 'Jared Diamond', 'The fates of human societies', 'HISTORY', 2, 2, '1997-03-01'),
('9780345816023', 'The Wright Brothers', 'David McCullough', 'Biography of aviation pioneers', 'BIOGRAPHY', 2, 1, '2015-05-05'),

-- Mystery Books
('9780345538987', 'The Girl with the Dragon Tattoo', 'Stieg Larsson', 'Swedish crime thriller', 'MYSTERY', 3, 2, '2005-08-01'),
('9780062073488', 'And Then There Were None', 'Agatha Christie', 'Classic mystery novel', 'MYSTERY', 2, 2, '1939-11-06');

-- Update statistics (for reporting)
-- This ensures our sample data is consistent
UPDATE books SET available_copies = total_copies - 1 WHERE isbn IN ('9780135166307', '9780544003415', '9780307387899', '9781250078100', '9780345816023', '9780345538987');

-- Verify data integrity
-- These should return 0 if all constraints are properly enforced
DO $$ 
BEGIN
    -- Check that no available_copies exceed total_copies
    IF EXISTS (SELECT 1 FROM books WHERE available_copies > total_copies) THEN
        RAISE EXCEPTION 'Data integrity violation: available_copies > total_copies';
    END IF;
    
    -- Check that all required fields are populated
    IF EXISTS (SELECT 1 FROM books WHERE title IS NULL OR title = '' OR author IS NULL OR author = '') THEN
        RAISE EXCEPTION 'Data integrity violation: missing required book fields';
    END IF;
    
    IF EXISTS (SELECT 1 FROM users WHERE email IS NULL OR email = '' OR first_name IS NULL OR first_name = '') THEN
        RAISE EXCEPTION 'Data integrity violation: missing required user fields';
    END IF;
    
    RAISE NOTICE 'Initial data inserted successfully - all integrity checks passed';
END $$;