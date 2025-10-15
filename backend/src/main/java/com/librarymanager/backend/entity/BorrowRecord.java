package com.librarymanager.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "borrow_records",
    indexes = {
        @Index(name = "idx_borrow_user_id", columnList = "user_id"),
        @Index(name = "idx_borrow_book_id", columnList = "book_id"),
        @Index(name = "idx_borrow_status", columnList = "status"),
        @Index(name = "idx_borrow_user_status", columnList = "user_id, status"),
        @Index(name = "idx_borrow_due_date", columnList = "due_date")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;
    
    @Column(name = "borrow_date", nullable = false)
    private LocalDate borrowDate;
    
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    @Column(name = "return_date")
    private LocalDate returnDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BorrowStatus status;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Convenience method to check if borrow is overdue
     */
    @Transient
    public boolean isOverdue() {
        return status == BorrowStatus.BORROWED && 
               LocalDate.now().isAfter(dueDate);
    }
    
    /**
     * Calculate due date (7 days from borrow date)
     * <p>
     * Calculates the due date for a borrow record. The current business rule
     * uses a fixed loan period of 7 days: the due date is computed as the
     * provided {@code borrowDate} plus 7 calendar days.
     * </p>
     *
     * <p>Notes:
     * <ul>
     *   <li>The method uses calendar days, not business days. If the project
     *       later requires skipping weekends/holidays, this method should be
     *       updated accordingly (or a policy/service introduced).</li>
     *   <li>Null input will produce a {@link NullPointerException} because
     *       the method delegates to {@link LocalDate#plusDays(long)}. Callers
     *       should validate input where appropriate.</li>
     * </ul>
     * </p>
     *
     * @param borrowDate the date when the book was borrowed; must not be null
     * @return the calculated due date which is exactly 7 days after {@code borrowDate}
     */
    public static LocalDate calculateDueDate(LocalDate borrowDate) {
        return borrowDate.plusDays(7);
    }
}