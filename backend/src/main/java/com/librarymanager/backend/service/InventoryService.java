package com.librarymanager.backend.service;

import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BorrowRecord;
import com.librarymanager.backend.entity.BorrowStatus;
import com.librarymanager.backend.entity.User;
import com.librarymanager.backend.exception.BusinessRuleViolationException;
import com.librarymanager.backend.exception.ResourceNotFoundException;
import com.librarymanager.backend.repository.BookRepository;
import com.librarymanager.backend.repository.BorrowRecordRepository;
import com.librarymanager.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for Book inventory operations.
 * 
 * <p>
 * This service provides business logic for book inventory management including:
 * <ul>
    * <li>Borrowing books</li>
    * <li>Returning books</li>
    * <li>Checking availability</li>
 * </ul>
 * </p>
 * 
 * @author Marcel Pulido
 * @version 1.0
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InventoryService {
    
    private final BorrowRecordRepository borrowRecordRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    /**
     *  Borrow a book for a user
     * 
     * <p>Business Rules:
     * <ol>
        * <li> User cannot borrow same book twice (checked by DB unique constraint)
        * <li> Book must have available copies
        * <li> Due date is automatically set to 7 days from borrow date
     * </ol>
     * </p>
     * 
     * @param userId Authenticated user ID (from JWT)
     * @param bookId the ID of the book to borrow
     * @return Created {@link BorrowRecord}
     * @throws ResourceNotFoundException if book not found
     * @throws BusinessRuleViolationException if book not available or if user already borrowed the book
     */
    public BorrowRecord borrowBook(Long userId, Long bookId) {
        log.debug("User {} attempting to borrow book {}", userId, bookId);
        
        User user = findUserById(userId);
        Book book = findBookById(bookId);
        
        // 2. Check if user already has this book borrowed
        boolean currentlyBorrowed = checkNoCurrentBorrow(userId, bookId);
        if (currentlyBorrowed) {
            log.warn("User {} already has book {} borrowed", userId, bookId);
            throw new BusinessRuleViolationException("You have already borrowed this book");
        }
        
        // 3. Check out a copy
        try {
            book.borrowCopy();
        } catch (IllegalStateException e) {
            log.warn("Book '{}' (ID: {}) is not available for borrowing", 
                book.getTitle(), bookId);
            throw BusinessRuleViolationException.bookNotAvailable(book.getTitle());
        }
        
        Book savedBook = bookRepository.save(book);
        BorrowRecord borrowRecord = createBorrowRecord(user, savedBook);
        
        BorrowRecord savedBorrowRecord = borrowRecordRepository.save(borrowRecord);
        log.info("User {} successfully borrowed book {} (due: {})", userId, bookId, savedBorrowRecord.getDueDate());
        
        return savedBorrowRecord;
    }

    /**
     * Return a borrowed book for a user.
     * 
     * @param userId Authenticated user ID (from JWT)
     * @param bookId the ID of the book to return
     * @return Updated {@link BorrowRecord}
     * @throws ResourceNotFoundException if user or book not found
     * @throws BusinessRuleViolationException if no active borrow record exists for user and book
     */
    public BorrowRecord returnBook(Long userId, Long bookId) {
        log.debug("User {} attempting to return book {}", userId, bookId);

        boolean bothExist = verifyUserAndBookExist(userId, bookId); 
        if (!bothExist) {
            log.warn("User {} or Book {} not found", userId, bookId);
            throw new ResourceNotFoundException("User or Book not found");
        }
        
        BorrowRecord borrowRecord = findActiveBorrowRecord(userId, bookId);   
        Book book = borrowRecord.getBook();

         try {
            // Use domain method from Book entity
            book.returnCopy();
         } catch (IllegalStateException e) {
            log.warn("Failed to return book '{}' (ID: {}): {}", book.getTitle(), bookId, e.getMessage());
            throw BusinessRuleViolationException.cannotReturnAllCopiesAvailable(book.getTitle());
         }

         Book updatedBook = bookRepository.save(book);
         log.info("Book returned successfully: '{}' (ID: {}). Available copies: {}", 
                book.getTitle(), bookId, updatedBook.getAvailableCopies());

        // 2. Update borrow record
        BorrowRecord updated = updateBorrowRecord(borrowRecord);
        
        boolean wasOverdue = LocalDate.now().isAfter(borrowRecord.getDueDate());
        log.info("User {} returned book {} (overdue: {})", userId, bookId, wasOverdue);
        
        return updated;
    }
   
    /**
     * Get user's borrow records filtered by status
     * 
     * @param userId User ID
     * @param status Filter by BORROWED or RETURNED
     * @param pageable Pagination parameters
     * @return Page of filtered borrow records
     */
    @Transactional(readOnly = true)
    public Page<BorrowRecord> getUserBorrowRecordsByStatus(
        Long userId, 
        BorrowStatus status, 
        Pageable pageable
    ) {
        if (!userRepository.findById(userId).isPresent()) {
            throw new ResourceNotFoundException("User not found");
        }

        return borrowRecordRepository.findByUserIdAndStatus(userId, status, pageable);
    }
    
    /**
     * Check if user has currently borrowed a specific book
     * Used by frontend to show "Return" vs "Borrow" button
     * 
     * @param userId User ID
     * @param bookId Book ID
     * @return true if user has active borrow of this book
     */
    @Transactional(readOnly = true)
    public boolean hasUserBorrowedBook(Long userId, Long bookId) {

        boolean bothExist = verifyUserAndBookExist(userId, bookId); 
        if (!bothExist) {
            log.warn("User {} or Book {} not found", userId, bookId);
            throw new ResourceNotFoundException("User or Book not found");
        }

        return borrowRecordRepository
            .findActiveBorrowByUserAndBook(userId, bookId)
            .isPresent();
    }

    // =========================== Helper Methods ===========================

    /**
     * Updates the borrow record to mark the book as returned.
     * Sets the return date to the current date and updates the status to {@link BorrowStatus#RETURNED}.
     *
     * @param borrowRecord the borrow record to update
     * @return the updated {@link BorrowRecord} instance
     */
    private BorrowRecord updateBorrowRecord(BorrowRecord borrowRecord) {
        borrowRecord.setReturnDate(LocalDate.now());
        borrowRecord.setStatus(BorrowStatus.RETURNED);
        return borrowRecordRepository.save(borrowRecord);
    }

    /**
     * Retrieves the active borrow record for a specific user and book.
     * <p>
     * This method attempts to find an active borrow record associated with the given
     * user ID and book ID. If no such record exists, or if the book has already been returned,
     * a {@link BusinessRuleViolationException} is thrown.
     *
     * @param userId the ID of the user who borrowed the book
     * @param bookId the ID of the book being borrowed
     * @return the active {@link BorrowRecord} for the specified user and book
     * @throws BusinessRuleViolationException if the user has not borrowed the book or it has already been returned
     */
    private BorrowRecord findActiveBorrowRecord(Long userId, Long bookId) {
        return borrowRecordRepository
                .findActiveBorrowByUserAndBook(userId, bookId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                "You have not borrowed this book or it has already been returned"
            ));
    }

    /**
     * Creates a new {@link BorrowRecord} for the specified user and book.
     * The borrow date is set to the current date, and the due date is set to 7 days after the borrow date.
     * The status of the borrow record is initialized as {@link BorrowStatus#BORROWED}.
     *
     * @param user the user who is borrowing the book
     * @param book the book being borrowed
     * @return a new {@link BorrowRecord} instance with the specified user, book, borrow date, due date, and status
     */
     private BorrowRecord createBorrowRecord(User user, Book book) {
        LocalDate borrowDate = LocalDate.now();
        
        return BorrowRecord.builder()
            .user(user)
            .book(book)
            .borrowDate(borrowDate)
            .dueDate(BorrowRecord.calculateDueDate(borrowDate))
            .status(BorrowStatus.BORROWED)
            .build();
    }

    /**
     * Retrieves a {@link Book} entity by its unique identifier.
     *
     * @param bookId the unique identifier of the book to retrieve
     * @return the {@link Book} entity associated with the given ID
     * @throws ResourceNotFoundException if no book is found with the specified ID
     */
    private Book findBookById(Long bookId) {
        return bookRepository.findById(bookId)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
    }

    /**
     * Retrieves a {@link User} entity by its unique identifier.
     *
     * @param userId the unique identifier of the user to retrieve
     * @return the {@link User} entity associated with the given ID
     * @throws ResourceNotFoundException if no user is found with the specified ID
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Checks if the specified user has already borrowed the specified book and returns {@code true}
     * if an active borrow record exists or {@code false} otherwise.
     *
     * @param userId the ID of the user attempting to borrow the book
     * @param bookId the ID of the book to be borrowed
     * @return {@code true} if the user has already borrowed the book, otherwise {@code false}
     */
    private boolean checkNoCurrentBorrow(Long userId, Long bookId) {
        Optional<BorrowRecord> existingBorrow = borrowRecordRepository
            .findActiveBorrowByUserAndBook(userId, bookId);
        
         return existingBorrow.isPresent(); 
    }

       /**
     * Verify that both user and book exist in the system.
     * 
     * @param userId the ID of the user
     * @param bookId the ID of the book
     * @return {@code true} if both exist, {@code false} otherwise
     */
    private boolean verifyUserAndBookExist(Long userId, Long bookId) {
        return (userRepository.existsById(userId) && bookRepository.existsById(bookId));
    }
}
