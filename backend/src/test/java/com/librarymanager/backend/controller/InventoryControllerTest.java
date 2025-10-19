package com.librarymanager.backend.controller;

import com.librarymanager.backend.dto.response.BorrowRecordResponse;
import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BorrowRecord;
import com.librarymanager.backend.entity.BorrowStatus;
import com.librarymanager.backend.entity.User;
import com.librarymanager.backend.exception.BusinessRuleViolationException;
import com.librarymanager.backend.exception.ResourceNotFoundException;
import com.librarymanager.backend.mapper.BorrowRecordMapper;
import com.librarymanager.backend.security.CustomUserDetails;
import com.librarymanager.backend.service.InventoryService;
import com.librarymanager.backend.testutil.TestDataFactory;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Nested;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.List;

/**
 * Unit tests for InventoryController.
 * 
 * Tests inventory management endpoints including book borrowing and returning functionality.
 * Follows Spring Boot testing best practices with proper mocking and assertions.
 * 
 * @author Marcel Pulido
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryController Unit Tests")
class InventoryControllerTest {

    private static final String NOT_AVAILABLE = "Book has no available copies";

    private static final String BOOK_NOT_FOUND = "Book not found";

    private static final String USER_NOT_FOUND = "User not found";

    private static final String ALREADY_BORROWED = "You have already borrowed this book";

    private static final String USER_BOOK_NOT_FOUND = "User or Book not found";

    private static final String RETURNED_OR_NOT_BORROWED = "You have not borrowed this book or it has already been returned";

    private static final String ALL_COPIES_AVAILABLE = "Cannot return book. All copies are already available";

    private static final Long VALID_USER_ID = 1L;

    private static final Long INVALID_USER_ID = 999L;
    
    private static final Long VALID_BOOK_ID = 1L;
    
    private static final Long INVALID_BOOK_ID = 999L;
    
    private static final Long VALID_BORROW_RECORD_ID = 1L;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private BorrowRecordMapper borrowRecordMapper;

    @InjectMocks
    private InventoryController inventoryController;

    private User user;
    private CustomUserDetails customUserDetails, invalidUserDetails;
    private Book book;
    List<Book> books;
    private BorrowRecord borrowRecordMock;
    private BorrowRecordResponse borrowRecordResponseMock, returnedBorrowRecordResponseMock;
    private Page<BorrowRecord> borrowRecordPageMock;
    private Page<BorrowRecordResponse> borrowRecordResponsePageMock;
    private Pageable pageable;
  
    @BeforeEach
    void setUp() {
        user = TestDataFactory.createDefaultMemberUser();
        user.setId(VALID_USER_ID);

        customUserDetails = TestDataFactory.createCustomUserDetails(user);

        book = TestDataFactory.createBookForBorrowing();
        book.setId(VALID_BOOK_ID); 

        borrowRecordMock = TestDataFactory.createBorrowRecord(user, book);
        borrowRecordMock.setId(VALID_BORROW_RECORD_ID);
        
        borrowRecordResponseMock = TestDataFactory.createBorrowRecordResponse(borrowRecordMock);
        returnedBorrowRecordResponseMock = TestDataFactory.createReturnedBorrowRecordResponse(borrowRecordMock);
    }

    // ========== Borrow Book Tests ==========

    @Nested
    @DisplayName("Borrow Book Tests")
    class BorrowBookTests {

        @Test
        @DisplayName("borrowBook_shouldReturnBorrowRecordResponse_whenValidBookIdAndValidUserIdProvided")
        void borrowBook_shouldReturnBorrowRecordResponse_whenValidBookIdAndValidUserIdProvided() {
            // Arrange
            when(inventoryService.borrowBook(VALID_USER_ID, VALID_BOOK_ID))
                .thenReturn(borrowRecordMock);
            when(borrowRecordMapper.toResponse(borrowRecordMock)).thenReturn(borrowRecordResponseMock);
            
            // Act
            ResponseEntity<BorrowRecordResponse> borrowRecordResponse = 
                        inventoryController.borrowBook(VALID_BOOK_ID, customUserDetails);

            // Assert
            assertThat(borrowRecordResponse).isNotNull();
            assertThat(borrowRecordResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(borrowRecordResponse.getBody().getBookId()).isEqualTo(VALID_BOOK_ID);
            assertThat(borrowRecordResponse.getBody().getStatus()).isEqualTo(BorrowStatus.BORROWED);
            assertThat(borrowRecordResponse.getBody()).isEqualTo(borrowRecordResponseMock);

            verify(inventoryService).borrowBook(VALID_BOOK_ID, VALID_BOOK_ID);
            verify(borrowRecordMapper).toResponse(borrowRecordMock);
        }

        @Test
        @DisplayName("borrowBook_shouldThrowResourceNotFoundException_whenInvalidBookIdProvided")
        void borrowBook_shouldThrowResourceNotFoundException_whenInvalidBookIdProvided() {
        // Given
        given(inventoryService.borrowBook(VALID_USER_ID, INVALID_BOOK_ID))
                    .willThrow(new ResourceNotFoundException(BOOK_NOT_FOUND));
        
        // When & Then
            assertThatThrownBy(() -> 
                inventoryController.borrowBook(INVALID_BOOK_ID, customUserDetails)
            )
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining(BOOK_NOT_FOUND);

            verify(inventoryService).borrowBook(VALID_USER_ID, INVALID_BOOK_ID);
            verify(borrowRecordMapper, never()).toResponse(any(BorrowRecord.class));
        }

        @Test
        @DisplayName("borrowBook_shouldThrowResourceNotFoundException_whenInvalidUserIdProvided")
        void borrowBook_shouldThrowResourceNotFoundException_whenInvalidUserIdProvided() {
            // Given
            given(inventoryService.borrowBook(INVALID_USER_ID, VALID_BOOK_ID))
                    .willThrow(new ResourceNotFoundException(USER_NOT_FOUND));

            user.setId(INVALID_USER_ID);
            invalidUserDetails = TestDataFactory.createCustomUserDetails(user);
            
            // When & Then
            assertThatThrownBy(() -> 
                    inventoryController.borrowBook(VALID_BOOK_ID, invalidUserDetails)
            )
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining(USER_NOT_FOUND);
        
            verify(inventoryService).borrowBook(INVALID_USER_ID, VALID_BOOK_ID);
            verify(borrowRecordMapper, never()).toResponse(any(BorrowRecord.class));
        }

        @ParameterizedTest
        @ValueSource(strings = {NOT_AVAILABLE, ALREADY_BORROWED})
        @DisplayName("borrowBook_shouldThrowBusinessRuleViolationException_whenNoCopiesAvailableOrBookAlreadyBorrowedByUser")
        void borrowBook_shouldThrowBusinessRuleViolationException_whenNoCopiesAvailableOrBookAlreadyBorrowedByUser(String exceptionMessage) {
            // Given
            given(inventoryService.borrowBook(VALID_USER_ID, VALID_BOOK_ID))
                    .willThrow(new BusinessRuleViolationException(exceptionMessage));

            // When & Then
            assertThatThrownBy(() -> 
                inventoryController.borrowBook(VALID_BOOK_ID, customUserDetails)
            )
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining(exceptionMessage);

            verify(inventoryService).borrowBook(VALID_USER_ID, VALID_BOOK_ID);
            verify(borrowRecordMapper, never()).toResponse(any(BorrowRecord.class));
        }
    }

    @Nested
    @DisplayName("Return Book Tests")
    class ReturnBookTests {

        @Test
        @DisplayName("returnBook_shouldReturnUpdatedBookResponse_whenValidBookIdAndValidUserIdProvided")
        void returnBook_shouldReturnUpdatedBookResponse_whenValidBookIdAndValidUserIdProvided() {
            // Arrange
            when(inventoryService.returnBook(VALID_USER_ID, VALID_BOOK_ID))
                .thenReturn(borrowRecordMock);
            when(borrowRecordMapper.toResponse(borrowRecordMock)).thenReturn(returnedBorrowRecordResponseMock);

            // Act
            ResponseEntity<BorrowRecordResponse> response = 
                        inventoryController.returnBook(VALID_BOOK_ID, customUserDetails);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getBookId()).isEqualTo(VALID_BOOK_ID);
            assertThat(response.getBody().getStatus()).isEqualTo(BorrowStatus.RETURNED);
            assertThat(response.getBody()).isEqualTo(returnedBorrowRecordResponseMock);

            verify(inventoryService).returnBook(VALID_USER_ID, VALID_BOOK_ID);
            verify(borrowRecordMapper).toResponse(borrowRecordMock);
        }

        @Test
        @DisplayName("returnBook_throwResourceNotFoundException_whenInvalidBookIdProvided")
        void returnBook_shouldCallServiceWithCorrectId_whenBookIdProvided() {
            // Given
            given(inventoryService.returnBook(VALID_USER_ID, INVALID_BOOK_ID))
                    .willThrow(new ResourceNotFoundException(USER_BOOK_NOT_FOUND));
            
            // When & Then
            assertThatThrownBy(() -> 
                inventoryController.returnBook(INVALID_BOOK_ID, customUserDetails)
            )
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining(USER_BOOK_NOT_FOUND);

            verify(inventoryService).returnBook(VALID_USER_ID, INVALID_BOOK_ID);
            verify(borrowRecordMapper, never()).toResponse(any(BorrowRecord.class));
        }

        @Test
        @DisplayName("returnBook_throwResourceNotFoundException_whenUserBookIdProvided")
        void returnBook_shouldIncreaseAvailableCopies_whenBookReturned() {
            // Given
            given(inventoryService.returnBook(INVALID_USER_ID, VALID_BOOK_ID))
                    .willThrow(new ResourceNotFoundException(USER_BOOK_NOT_FOUND));

            user.setId(INVALID_USER_ID);
            invalidUserDetails = TestDataFactory.createCustomUserDetails(user);
            
            // When & Then
            assertThatThrownBy(() -> 
                    inventoryController.returnBook(VALID_BOOK_ID, invalidUserDetails)
            )
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining(USER_BOOK_NOT_FOUND);
        
            verify(inventoryService).returnBook(INVALID_USER_ID, VALID_BOOK_ID);
            verify(borrowRecordMapper, never()).toResponse(any(BorrowRecord.class));
        }

        @ParameterizedTest
        @ValueSource(strings = {RETURNED_OR_NOT_BORROWED, ALL_COPIES_AVAILABLE})
        @DisplayName("returnBook_throwBusinessRuleViolationException_whenBookReturnedByUserOrNeverBorrowedOrAllCopiesAvailable")
        void returnBook_shouldHandleBookWithAllCopiesAvailable_whenReturningBook(String exceptionMessage) {
            // Given
            given(inventoryService.returnBook(VALID_USER_ID, VALID_BOOK_ID))
                    .willThrow(new BusinessRuleViolationException(exceptionMessage));

            // When & Then
            assertThatThrownBy(() -> 
                inventoryController.returnBook(VALID_BOOK_ID, customUserDetails)
            )
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining(exceptionMessage);

            verify(inventoryService).returnBook(VALID_USER_ID, VALID_BOOK_ID);
            verify(borrowRecordMapper, never()).toResponse(any(BorrowRecord.class));
        }
    }

    @Nested
    @DisplayName("Check Borrow Status Tests")
    class CheckBorrowStatusTests {

        @Test
        @DisplayName("checkBorrowStatus_shouldReturnTrue_whenUserHasBorrowedBook")
        void checkBorrowStatus_shouldReturnTrue_whenUserHasBorrowedBook() {
            // Arrange
            when(inventoryService.hasUserBorrowedBook(VALID_USER_ID, VALID_BOOK_ID))
                .thenReturn(true);

            // Act
            ResponseEntity<InventoryController.BorrowStatusResponse> response = 
                        inventoryController.checkBorrowStatus(VALID_BOOK_ID, customUserDetails);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().borrowed()).isTrue();

            verify(inventoryService).hasUserBorrowedBook(VALID_USER_ID, VALID_BOOK_ID);
        }

        @Test
        @DisplayName("checkBorrowStatus_shouldReturnFalse_whenUserHasNotBorrowedBook")
        void checkBorrowStatus_shouldReturnFalse_whenUserHasNotBorrowedBook() {
            // Arrange
            when(inventoryService.hasUserBorrowedBook(VALID_USER_ID, VALID_BOOK_ID))
                .thenReturn(false);

            // Act
            ResponseEntity<InventoryController.BorrowStatusResponse> response = 
                        inventoryController.checkBorrowStatus(VALID_BOOK_ID, customUserDetails);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().borrowed()).isFalse();

            verify(inventoryService).hasUserBorrowedBook(VALID_USER_ID, VALID_BOOK_ID);
        }

        @Test
        @DisplayName("checkBorrowStatus_shouldThrowResourceNotFoundException_whenInvalidUserIdProvided")
        void checkBorrowStatus_shouldThrowResourceNotFoundException_whenInvalidUserIdProvided() {
            // Given
            given(inventoryService.hasUserBorrowedBook(INVALID_USER_ID, VALID_BOOK_ID))
                    .willThrow(new ResourceNotFoundException(USER_NOT_FOUND));

            user.setId(INVALID_USER_ID);
            invalidUserDetails = TestDataFactory.createCustomUserDetails(user);
            
            // When & Then
            assertThatThrownBy(() -> 
                    inventoryController.checkBorrowStatus(VALID_BOOK_ID, invalidUserDetails)
            )
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining(USER_NOT_FOUND);
        
            verify(inventoryService).hasUserBorrowedBook(INVALID_USER_ID, VALID_BOOK_ID);
            verifyNoMoreInteractions(inventoryService);
        }
    }

    @Nested
    @DisplayName("User Borrow Records Tests")
    class UserBorrowRecordsTests {

        @BeforeEach
        void setup() {
            books = Arrays.asList(book);
        }

        @Test
        @DisplayName("getUserBorrowRecords_shouldReturnPageOfBorrowRecordResponses_whenValidUserIdProvided")
        void getUserBorrowRecords_shouldReturnPageOfBorrowRecordResponses_whenValidUserIdProvided() {
            // Arrange
            pageable = TestDataFactory.createDefaultPageable();
            borrowRecordPageMock = TestDataFactory.createDefaultBorrowRecordPage(user, books);
            borrowRecordResponsePageMock = TestDataFactory.createDefaultBorrowRecordResponsePage(user, books);

            when(inventoryService.getUserBorrowRecordsByStatus(VALID_USER_ID, BorrowStatus.BORROWED, pageable))
                .thenReturn(borrowRecordPageMock);

            when(borrowRecordMapper.toResponse(any(BorrowRecord.class)))
            .thenAnswer(invocation -> {
                BorrowRecord record = invocation.getArgument(0);
                return borrowRecordResponsePageMock.getContent().stream()
                        .filter(resp -> resp.getId().equals(record.getId()))
                        .findFirst()
                        .orElse(borrowRecordResponsePageMock.getContent().get(0));
            });
        
            // Act
            ResponseEntity<Page<BorrowRecordResponse>> response = 
                        inventoryController.getUserBorrowRecords(BorrowStatus.BORROWED, 0, 10, customUserDetails);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getTotalElements()).isEqualTo(borrowRecordResponsePageMock.getTotalElements());
            assertThat(response.getBody()).isEqualTo(borrowRecordResponsePageMock);
            
            verify(inventoryService).getUserBorrowRecordsByStatus(VALID_USER_ID, BorrowStatus.BORROWED, pageable);
            verify(borrowRecordMapper, times(borrowRecordPageMock.getNumberOfElements())).toResponse(any(BorrowRecord.class));

        }

        @Test
        @DisplayName("getUserBorrowRecords_shouldThrowResourceNotFoundException_whenInvalidUserIdProvided")
        void getUserBorrowRecords_shouldThrowResourceNotFoundException_whenInvalidUserIdProvided() {
            // Given
            pageable = TestDataFactory.createDefaultPageable();
            given(inventoryService.getUserBorrowRecordsByStatus(INVALID_USER_ID, BorrowStatus.BORROWED, pageable))
                    .willThrow(new ResourceNotFoundException(USER_NOT_FOUND));

            user.setId(INVALID_USER_ID);
            invalidUserDetails = TestDataFactory.createCustomUserDetails(user);
            
            // When & Then
            assertThatThrownBy(() -> 
                    inventoryController.getUserBorrowRecords(BorrowStatus.BORROWED, 0, 10, invalidUserDetails)
            )
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining(USER_NOT_FOUND);
        
            verify(inventoryService).getUserBorrowRecordsByStatus(INVALID_USER_ID, BorrowStatus.BORROWED, pageable);
            verifyNoMoreInteractions(inventoryService);
            verify(borrowRecordMapper, never()).toResponse(any(BorrowRecord.class));
        }

         @Test
        @DisplayName("getUserBorrowRecords_shouldReturnEmptyPage_whenUserDoesNotHaveAnyBorrowedBooks")
        void getUserBorrowRecords_shouldReturnEmptyPage_whenUserDoesNotHaveAnyBorrowedBooks() {
            // Given
            pageable = TestDataFactory.createDefaultPageable();
            given(inventoryService.getUserBorrowRecordsByStatus(VALID_USER_ID, BorrowStatus.BORROWED, pageable))
                    .willReturn(TestDataFactory.createEmptyBorrowRecordPage());

            // When
            ResponseEntity<Page<BorrowRecordResponse>> response = inventoryController.getUserBorrowRecords(BorrowStatus.BORROWED, 0, 10, customUserDetails);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getTotalElements()).isEqualTo(0);
            assertThat(response.getBody().getContent()).isEmpty();

            verify(inventoryService).getUserBorrowRecordsByStatus(VALID_USER_ID, BorrowStatus.BORROWED, pageable);
            verify(borrowRecordMapper, never()).toResponse(any(BorrowRecord.class));
        }
        
        @Test
        @DisplayName("getUserBorrowRecords_shouldReturnPageOfBorrowRecordResponses_whenPageAndSizeProvided")
        void getUserBorrowRecords_shouldReturnPageOfBorrowRecordResponses_whenPageAndSizeProvided() {
            // Given
            pageable = TestDataFactory.createPageable(1, 15);
            borrowRecordPageMock = TestDataFactory.createCustomBorrowRecordPage(user, books, pageable.getPageNumber(), pageable.getPageSize());
            borrowRecordResponsePageMock = TestDataFactory.createCustomBorrowRecordResponsePage(user, books, pageable.getPageNumber(), pageable.getPageSize());
            given(inventoryService.getUserBorrowRecordsByStatus(VALID_USER_ID, BorrowStatus.BORROWED, pageable))
                    .willReturn(borrowRecordPageMock);

             when(borrowRecordMapper.toResponse(any(BorrowRecord.class)))
            .thenAnswer(invocation -> {
                BorrowRecord record = invocation.getArgument(0);
                return borrowRecordResponsePageMock.getContent().stream()
                        .filter(resp -> resp.getId().equals(record.getId()))
                        .findFirst()
                        .orElse(borrowRecordResponsePageMock.getContent().get(0));
            });

            // When
            ResponseEntity<Page<BorrowRecordResponse>> response = inventoryController.getUserBorrowRecords(BorrowStatus.BORROWED, pageable.getPageNumber(), pageable.getPageSize(), customUserDetails);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getSize()).isEqualTo(pageable.getPageSize());
            assertThat(response.getBody().getNumber()).isEqualTo(pageable.getPageNumber());
            assertThat(response.getBody().getTotalElements()).isEqualTo(borrowRecordResponsePageMock.getTotalElements());
            assertThat(response.getBody()).isEqualTo(borrowRecordResponsePageMock);

            verify(inventoryService).getUserBorrowRecordsByStatus(VALID_USER_ID, BorrowStatus.BORROWED, pageable);
            verify(borrowRecordMapper, times(borrowRecordPageMock.getNumberOfElements())).toResponse(any(BorrowRecord.class));
        }
    }
}
