-- V5__Seed_borrow_records.sql

-- Seed some borrow records for testing
-- Assuming user ID 1 exists
-- Assuming books 1-4 exist 

-- Active borrow (due soon)
INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date, return_date, status, created_at, updated_at)
VALUES (2, 1, CURRENT_DATE - INTERVAL '5 days', CURRENT_DATE + INTERVAL '2 days', NULL, 'BORROWED', NOW(), NOW());

-- Active borrow (overdue)
INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date, return_date, status, created_at, updated_at)
VALUES (2, 2, CURRENT_DATE - INTERVAL '10 days', CURRENT_DATE - INTERVAL '3 days', NULL, 'BORROWED', NOW(), NOW());

-- Returned book (on time)
INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date, return_date, status, created_at, updated_at)
VALUES (2, 3, CURRENT_DATE - INTERVAL '20 days', CURRENT_DATE - INTERVAL '13 days', CURRENT_DATE - INTERVAL '12 days', 'RETURNED', NOW(), NOW());

-- Returned book (late)
INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date, return_date, status, created_at, updated_at)
VALUES (2, 4, CURRENT_DATE - INTERVAL '30 days', CURRENT_DATE - INTERVAL '23 days', CURRENT_DATE - INTERVAL '20 days', 'RETURNED', NOW(), NOW());

-- Update book available copies to reflect borrows
UPDATE books SET available_copies = available_copies - 1 WHERE id IN (1, 2) AND available_copies > 0;