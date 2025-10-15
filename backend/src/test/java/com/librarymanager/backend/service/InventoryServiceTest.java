package com.librarymanager.backend.service;

import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.entity.BorrowRecord;
import com.librarymanager.backend.entity.BorrowStatus;
import com.librarymanager.backend.entity.User;
import com.librarymanager.backend.repository.BookRepository;
import com.librarymanager.backend.repository.BorrowRecordRepository;
import com.librarymanager.backend.repository.UserRepository;
import com.librarymanager.backend.testutil.TestDataFactory;
import com.librarymanager.backend.exception.BusinessRuleViolationException;
import com.librarymanager.backend.exception.ResourceNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InventoryService focusing on book borrowing/returning domain logic.
 * 
 * Key Testing Focus:
 * - Domain-driven design validation (Book.borrowCopy(), Book.returnCopy())
 * - Service orchestration between InventoryService and BookCatalogService
 * - Business rule enforcement and error handling
 * - State changes and side effects verification
 * 
 * @author Marcel Pulido
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Unit Tests")
class InventoryServiceTest {

    @Mock
    private BorrowRecordRepository borrowRecordRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private User testUser;
    private Book availableBook;
    private Book unavailableBook;
    private Book singleCopyBook;
    private BorrowRecord activeBorrowRecord;
    private BorrowRecord returnedBorrowRecord;

    @BeforeEach
    void setUp() {
        // Set up test user
        testUser = User.builder()
            .email("test@example.com")
            .firstName("Test")
            .lastName("User")
            .build();
        testUser.setId(1L);

        // Set up test books
        availableBook = TestDataFactory.createCustomBook(
            "978-0134685991",
            "Effective Java",
            "Joshua Bloch",
            "Best practices for Java programming",
            LocalDate.of(2017, 12, 27),
            BookGenre.TECHNOLOGY,
            5,
            3);
        availableBook.setId(1L);

        unavailableBook = TestDataFactory.createCustomBook(
            "978-0321356680",
            "Clean Code",
            "Robert Martin",
            "A Handbook of Agile Software Craftsmanship",
            LocalDate.of(2008, 8, 1),
            BookGenre.TECHNOLOGY,
            2,
            0);    
        unavailableBook.setId(2L);

        singleCopyBook = TestDataFactory.createCustomBook(
            "978-0201633610",
            "Design Patterns",
            "Gang of Four",
            "Elements of Reusable Object-Oriented Software",
            LocalDate.of(1994, 10, 31),
            BookGenre.TECHNOLOGY,
            1,
            1);
        singleCopyBook.setId(3L);

        // Set up borrow records
        LocalDate now = LocalDate.now();
        activeBorrowRecord = BorrowRecord.builder()
            .id(1L)
            .user(testUser)
            .book(unavailableBook)
            .borrowDate(now)
            .dueDate(now.plusDays(7))
            .status(BorrowStatus.BORROWED)
            .build();

        returnedBorrowRecord = BorrowRecord.builder()
            .id(2L)
            .user(testUser)
            .book(availableBook)
            .borrowDate(now.minusDays(14))
            .dueDate(now.minusDays(7))
            .returnDate(now.minusDays(6))
            .status(BorrowStatus.RETURNED)
            .build();
    }

    // ========== borrowBook Tests ==========

    @Test
    @DisplayName("Should successfully borrow book when available")
    void borrowBook_BookAvailable_UpdatesInventoryAndReturnsRecord() {
        // Given
        Long userId = 1L;
        Long bookId = 1L;
        Book bookAfterBorrow = TestDataFactory.createCustomBook(
            availableBook.getIsbn(), availableBook.getTitle(), availableBook.getAuthor(), availableBook.getDescription(), availableBook.getPublicationDate(),
            availableBook.getGenre(), 5, 2); // Decremented by 1
        bookAfterBorrow.setId(1L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(availableBook));
        when(borrowRecordRepository.findActiveBorrowByUserAndBook(userId, bookId)).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenReturn(bookAfterBorrow);
        when(borrowRecordRepository.save(any(BorrowRecord.class))).thenAnswer(invocation -> {
            BorrowRecord record = invocation.getArgument(0);
            record.setId(1L);
            return record;
        });

        // When
        BorrowRecord result = inventoryService.borrowBook(userId, bookId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getBook().getAvailableCopies()).isEqualTo(2);
        assertThat(result.getStatus()).isEqualTo(BorrowStatus.BORROWED);
        assertThat(result.getDueDate()).isEqualTo(result.getBorrowDate().plusDays(7));
        
        verify(userRepository).findById(userId);
        verify(bookRepository).findById(bookId);
        verify(borrowRecordRepository).findActiveBorrowByUserAndBook(userId, bookId);
        verify(bookRepository).save(any(Book.class));
        verify(borrowRecordRepository).save(any(BorrowRecord.class));
    }

    @Test
    @DisplayName("Should successfully borrow last available copy")
    void borrowBook_LastCopy_UpdatesInventoryToZero() {
        // Given
        Long userId = 1L;
        Long bookId = 3L;
        Book bookAfterBorrow = TestDataFactory.createCustomBook(
            singleCopyBook.getIsbn(), singleCopyBook.getTitle(), singleCopyBook.getAuthor(), singleCopyBook.getDescription(), singleCopyBook.getPublicationDate(),
            singleCopyBook.getGenre(),
            1, 0); // Last copy borrowed
        bookAfterBorrow.setId(3L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(singleCopyBook));
        when(borrowRecordRepository.findActiveBorrowByUserAndBook(userId, bookId)).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenReturn(bookAfterBorrow);
        when(borrowRecordRepository.save(any(BorrowRecord.class))).thenAnswer(invocation -> {
            BorrowRecord record = invocation.getArgument(0);
            record.setId(2L);
            return record;
        });

        // When
        BorrowRecord result = inventoryService.borrowBook(userId, bookId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getBook().getAvailableCopies()).isEqualTo(0);
        assertThat(result.getBook().getTotalCopies()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(BorrowStatus.BORROWED);
        
        verify(userRepository).findById(userId);
        verify(bookRepository).findById(bookId);
        verify(borrowRecordRepository).findActiveBorrowByUserAndBook(userId, bookId);
        verify(bookRepository).save(any(Book.class));
        verify(borrowRecordRepository).save(any(BorrowRecord.class));
    }

    @Test
    @DisplayName("Should throw exception when book not found")
    void borrowBook_BookNotFound_ThrowsException() {
        // Given
        Long userId = 1L;
        Long bookId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> inventoryService.borrowBook(userId, bookId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Book not found");

        verify(userRepository).findById(userId);
        verify(bookRepository).findById(bookId);
        verify(borrowRecordRepository, never()).findActiveBorrowByUserAndBook(any(), any());
        verify(bookRepository, never()).save(any());
        verify(borrowRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when book not available")
    void borrowBook_BookNotAvailable_ThrowsException() {
        // Given
        Long userId = 1L;
        Long bookId = 2L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(unavailableBook));
        when(borrowRecordRepository.findActiveBorrowByUserAndBook(userId, bookId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> inventoryService.borrowBook(userId, bookId))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("Book 'Clean Code' is not available for borrowing");

        verify(userRepository).findById(userId);
        verify(bookRepository).findById(bookId);
        verify(borrowRecordRepository).findActiveBorrowByUserAndBook(userId, bookId);
        verify(bookRepository, never()).save(any());
        verify(borrowRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user already has book borrowed")
    void borrowBook_AlreadyBorrowed_ThrowsException() {
        // Given
        Long userId = 1L;
        Long bookId = 2L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(unavailableBook));
        when(borrowRecordRepository.findActiveBorrowByUserAndBook(userId, bookId))
            .thenReturn(Optional.of(activeBorrowRecord));

        // When & Then
        assertThatThrownBy(() -> inventoryService.borrowBook(userId, bookId))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("You have already borrowed this book");

        verify(userRepository).findById(userId);
        verify(bookRepository).findById(bookId);
        verify(borrowRecordRepository).findActiveBorrowByUserAndBook(userId, bookId);
        verify(bookRepository, never()).save(any());
        verify(borrowRecordRepository, never()).save(any(BorrowRecord.class));
    }

    // ========== returnBook Tests ==========

    @Test
    @DisplayName("Should successfully return book when valid")
    void returnBook_ValidReturn_UpdatesInventoryAndReturnsBorrowRecord() {
        // Given
        Long userId = 1L;
        Long bookId = 2L;
        Book bookBeforeReturn = TestDataFactory.createCustomBook(
            unavailableBook.getIsbn(), unavailableBook.getTitle(), unavailableBook.getAuthor(),
            unavailableBook.getDescription(), unavailableBook.getPublicationDate(), unavailableBook.getGenre(), 
            2, 0); // All copies borrowed
        bookBeforeReturn.setId(2L);

        Book bookAfterReturn = TestDataFactory.createCustomBook(
            unavailableBook.getIsbn(), unavailableBook.getTitle(), unavailableBook.getAuthor(),
            unavailableBook.getDescription(), unavailableBook.getPublicationDate(), unavailableBook.getGenre(),
            2, 1); // One copy returned
        bookAfterReturn.setId(2L);

        BorrowRecord borrowBeforeReturn = activeBorrowRecord;
        BorrowRecord expectedReturnedRecord = BorrowRecord.builder()
            .id(1L)
            .user(testUser)
            .book(bookAfterReturn)
            .borrowDate(borrowBeforeReturn.getBorrowDate())
            .dueDate(borrowBeforeReturn.getDueDate())
            .returnDate(LocalDate.now())
            .status(BorrowStatus.RETURNED)
            .build();

        when(borrowRecordRepository.findActiveBorrowByUserAndBook(userId, bookId))
            .thenReturn(Optional.of(borrowBeforeReturn));
        when(bookRepository.save(any(Book.class))).thenReturn(bookAfterReturn);
        when(borrowRecordRepository.save(any(BorrowRecord.class))).thenReturn(expectedReturnedRecord);

        // When
        BorrowRecord result = inventoryService.returnBook(userId, bookId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(BorrowStatus.RETURNED);
        assertThat(result.getReturnDate()).isEqualTo(LocalDate.now());
        assertThat(result.getBook().getAvailableCopies()).isEqualTo(1);
        
        verify(borrowRecordRepository).findActiveBorrowByUserAndBook(userId, bookId);
        verify(bookRepository).save(any(Book.class));
        verify(borrowRecordRepository).save(any(BorrowRecord.class));
    }

    @Test
    @DisplayName("Should throw exception when no active borrow found")
    void returnBook_NoActiveBorrow_ThrowsException() {
        // Given
        Long userId = 1L;
        Long bookId = 999L;
        when(borrowRecordRepository.findActiveBorrowByUserAndBook(userId, bookId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> inventoryService.returnBook(userId, bookId))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("You have not borrowed this book or it has already been returned");

        verify(borrowRecordRepository).findActiveBorrowByUserAndBook(userId, bookId);
        verify(bookRepository, never()).save(any());
        verify(borrowRecordRepository, never()).save(any());
    }

    // ========== User Borrow Records Tests ==========
    
    @Test
    @DisplayName("Should retrieve user's borrow records with pagination")
    void getUserBorrowRecords_WithPagination_ReturnsBorrowRecords() {
        // Given
        Long userId = 1L;
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<BorrowRecord> records = Arrays.asList(activeBorrowRecord, returnedBorrowRecord);
        Page<BorrowRecord> expectedPage = new PageImpl<>(records, pageRequest, records.size());
        
        when(borrowRecordRepository.findByUserIdWithBook(userId, pageRequest)).thenReturn(expectedPage);

        // When
        Page<BorrowRecord> result = inventoryService.getUserBorrowRecords(userId, pageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).contains(activeBorrowRecord, returnedBorrowRecord);
        
        verify(borrowRecordRepository).findByUserIdWithBook(userId, pageRequest);
    }

    @Test
    @DisplayName("Should retrieve user's borrow records filtered by status")
    void getUserBorrowRecordsByStatus_FiltersByStatus_ReturnsBorrowRecords() {
        // Given
        Long userId = 1L;
        BorrowStatus status = BorrowStatus.BORROWED;
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<BorrowRecord> records = Arrays.asList(activeBorrowRecord);
        Page<BorrowRecord> expectedPage = new PageImpl<>(records, pageRequest, records.size());
        
        when(borrowRecordRepository.findByUserIdAndStatus(userId, status, pageRequest)).thenReturn(expectedPage);

        // When
        Page<BorrowRecord> result = inventoryService.getUserBorrowRecordsByStatus(userId, status, pageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(BorrowStatus.BORROWED);
        
        verify(borrowRecordRepository).findByUserIdAndStatus(userId, status, pageRequest);
    }

    @Test
    @DisplayName("Should correctly check if user has borrowed a book")
    void hasUserBorrowedBook_WithActiveBorrow_ReturnsTrue() {
        // Given
        Long userId = 1L;
        Long bookId = 2L;
        when(borrowRecordRepository.findActiveBorrowByUserAndBook(userId, bookId))
            .thenReturn(Optional.of(activeBorrowRecord));

        // When
        boolean result = inventoryService.hasUserBorrowedBook(userId, bookId);

        // Then
        assertThat(result).isTrue();
        verify(borrowRecordRepository).findActiveBorrowByUserAndBook(userId, bookId);
    }

    @Test
    @DisplayName("Should correctly check if user has not borrowed a book")
    void hasUserBorrowedBook_WithNoActiveBorrow_ReturnsFalse() {
        // Given
        Long userId = 1L;
        Long bookId = 2L;
        when(borrowRecordRepository.findActiveBorrowByUserAndBook(userId, bookId))
            .thenReturn(Optional.empty());

        // When
        boolean result = inventoryService.hasUserBorrowedBook(userId, bookId);

        // Then
        assertThat(result).isFalse();
        verify(borrowRecordRepository).findActiveBorrowByUserAndBook(userId, bookId);
    }
}