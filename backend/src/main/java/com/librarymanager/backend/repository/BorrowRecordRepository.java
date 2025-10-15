package com.librarymanager.backend.repository;

import com.librarymanager.backend.entity.BorrowRecord;
import com.librarymanager.backend.entity.BorrowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

  /**
     * Repository for managing {@link BorrowRecord} entities.
     *
     * <p>This interface provides commonly used queries to retrieve borrow
     * records for users and books, including specialized queries that
     * eagerly fetch related {@link com.librarymanager.backend.entity.Book}
     * entities to avoid the N+1 select problem.</p>
     */
@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {
  

    /**
     * Find all borrow records for a specific user with pagination.
     * Eagerly fetches the {@code book} association to avoid N+1 queries.
     *
     * @param userId   the id of the user whose borrow records will be returned
     * @param pageable pagination and sorting information
     * @return a {@link org.springframework.data.domain.Page} of {@link BorrowRecord}
     *         for the requested user, ordered by due date ascending
     */
    @Query("SELECT br FROM BorrowRecord br " +
           "JOIN FETCH br.book " +
           "WHERE br.user.id = :userId " +
           "ORDER BY br.dueDate ASC")
    Page<BorrowRecord> findByUserIdWithBook(@Param("userId") Long userId, Pageable pageable);
    
       /**
        * Find a user's borrow records filtered by status with pagination.
        * Eagerly fetches the {@code book} association.
        *
        * @param userId   the id of the user
        * @param status   the borrow status to filter by (e.g. BORROWED, RETURNED)
        * @param pageable pagination and sorting information
        * @return a {@link org.springframework.data.domain.Page} of matching {@link BorrowRecord}
        */
       @Query("SELECT br FROM BorrowRecord br " +
                 "JOIN FETCH br.book " +
                 "WHERE br.user.id = :userId AND br.status = :status " +
                 "ORDER BY br.dueDate ASC")
       Page<BorrowRecord> findByUserIdAndStatus(
              @Param("userId") Long userId, 
              @Param("status") BorrowStatus status, 
              Pageable pageable
       );
    
       /**
        * Find an active borrow record for a user and a specific book.
        *
        * <p>This query is used to determine whether the user currently has an
        * active borrow of the given book (status = BORROWED), which is
        * important to prevent duplicate borrows of the same copy.</p>
        *
        * @param userId the id of the user
        * @param bookId the id of the book
        * @return an {@link Optional} containing the active {@link BorrowRecord}
        *         if present, otherwise {@link Optional#empty()}
        */
       @Query("SELECT br FROM BorrowRecord br " +
                 "WHERE br.user.id = :userId " +
                 "AND br.book.id = :bookId " +
                 "AND br.status = 'BORROWED'")
       Optional<BorrowRecord> findActiveBorrowByUserAndBook(
              @Param("userId") Long userId, 
              @Param("bookId") Long bookId
       );
    
       /**
        * Count borrow records for a user filtered by status.
        *
        * @param userId the id of the user
        * @param status the borrow status to count
        * @return the number of borrow records that match the criteria
        */
       long countByUserIdAndStatus(Long userId, BorrowStatus status);
    
    /**
     * Find all borrow records that are currently overdue.
     *
     * <p>Overdue means the record is in status {@code BORROWED} and its
     * {@code dueDate} is strictly before the provided {@code currentDate}.</p>
     *
     * @param currentDate the reference date to determine overdue records
     * @return a list of overdue {@link BorrowRecord} instances
     */
    @Query("SELECT br FROM BorrowRecord br " +
           "WHERE br.status = 'BORROWED' " +
           "AND br.dueDate < :currentDate")
    List<BorrowRecord> findOverdueBorrows(@Param("currentDate") LocalDate currentDate);
    
       /**
        * Check if a specific book is currently borrowed by any user.
        *
        * <p>Useful for preventing deletion of a {@code Book} that still has
        * active borrow records.</p>
        *
        * @param bookId the id of the book
        * @param status the borrow status to check (typically {@code BORROWED})
        * @return {@code true} if at least one borrow record exists for the
        *         given book with the specified status, otherwise {@code false}
        */
       boolean existsByBookIdAndStatus(Long bookId, BorrowStatus status);
}