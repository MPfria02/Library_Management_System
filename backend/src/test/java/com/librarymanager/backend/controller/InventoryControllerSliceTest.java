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
import com.librarymanager.backend.security.JwtAuthenticationFilter;
import com.librarymanager.backend.service.InventoryService;
import com.librarymanager.backend.testutil.TestDataFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.data.domain.Pageable;

// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.BDDMockito.given;
// import static org.mockito.BDDMockito.willThrow;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// /**
//  * Slice tests for InventoryController using @WebMvcTest.
//  * 
//  * Tests the web layer in isolation, focusing on HTTP behavior,
//  * request/response mapping, and exception handling for inventory operations.
//  * 
//  * @author Marcel Pulido
//  */
@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("InventoryController Slice Tests")
class InventoryControllerSliceTest {

    private static final String BASE_URL = "/api/inventory/books";

    private static final String NOT_AVAILABLE = "Book has no available copies";
    private static final String BOOK_NOT_FOUND = "Book not found";
    private static final String USER_NOT_FOUND = "User not found";
    private static final String ALREADY_BORROWED = "You have already borrowed this book";
    private static final String RETURNED_OR_NOT_BORROWED = "You have not borrowed this book or it has already been returned";
    private static final String ALL_COPIES_AVAILABLE = "Cannot return book. All copies are already available";

    private static final Long VALID_USER_ID = 1L;
    private static final Long VALID_BOOK_ID = 1L;
    private static final Long INVALID_BOOK_ID = 999L;
    private static final Long VALID_BORROW_RECORD_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private BorrowRecordMapper borrowRecordMapper;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User testUser;
    private CustomUserDetails userDetails;
    private Book testBook;
    private BorrowRecord activeBorrowRecord;
    private BorrowRecordResponse borrowRecordResponse;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createDefaultMemberUser();
        testUser.setId(VALID_USER_ID);
        userDetails = TestDataFactory.createCustomUserDetails(testUser);

        testBook = TestDataFactory.createBookForBorrowing();
        testBook.setId(VALID_BOOK_ID);

        activeBorrowRecord = TestDataFactory.createBorrowRecord(testUser, testBook);
        activeBorrowRecord.setId(VALID_BORROW_RECORD_ID);
        borrowRecordResponse = TestDataFactory.createBorrowRecordResponse(activeBorrowRecord);

        // Mock authentication
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ===================== Helpers =====================
    private ResultActions borrowBook(Long bookId) throws Exception {
        return mockMvc.perform(post(BASE_URL + "/{id}/borrow", bookId)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions returnBook(Long bookId) throws Exception {
        return mockMvc.perform(post(BASE_URL + "/{id}/return", bookId)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions checkBorrowStatus(Long bookId) throws Exception {
        return mockMvc.perform(get(BASE_URL + "/{id}/status", bookId)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions getUserBorrowRecords() throws Exception {
        return mockMvc.perform(get(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions getUserBorrowRecordsByStatus(String status) throws Exception {
        return mockMvc.perform(get(BASE_URL)
                .param("status", status)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions getUserBorrowRecordsWithPagination(int page, int size) throws Exception {
        return mockMvc.perform(get(BASE_URL)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .contentType(MediaType.APPLICATION_JSON));
    }

    @Nested
    @DisplayName("Borrow Book Tests")
    class BorrowBookTests {

        @Test
        @DisplayName("Should return 200 and borrow record when book is successfully borrowed")
        void borrowBook_ValidRequest_Returns200AndBorrowRecord() throws Exception {
            // Given
            when(inventoryService.borrowBook(VALID_USER_ID, VALID_BOOK_ID))
                .thenReturn(activeBorrowRecord);
            when(borrowRecordMapper.toResponse(activeBorrowRecord))
                .thenReturn(borrowRecordResponse);

            // When & Then
            borrowBook(VALID_BOOK_ID)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(activeBorrowRecord.getId()))
                    .andExpect(jsonPath("$.bookId").value(VALID_BOOK_ID))
                    .andExpect(jsonPath("$.status").value("BORROWED"));

            verify(inventoryService).borrowBook(VALID_USER_ID, VALID_BOOK_ID);
            verify(borrowRecordMapper).toResponse(activeBorrowRecord);
        }

        @Test
        @DisplayName("Should return 404 when book is not found")
        void borrowBook_BookNotFound_Returns404() throws Exception {
            // Given
            when(inventoryService.borrowBook(VALID_USER_ID, INVALID_BOOK_ID))
                .thenThrow(new ResourceNotFoundException(BOOK_NOT_FOUND));

            // When & Then
            borrowBook(INVALID_BOOK_ID)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(BOOK_NOT_FOUND));

            verify(inventoryService).borrowBook(VALID_USER_ID, INVALID_BOOK_ID);
            verify(borrowRecordMapper, never()).toResponse(any());
        }

        @ParameterizedTest
        @ValueSource(strings = {NOT_AVAILABLE, ALREADY_BORROWED})
        @DisplayName("Should return 422 when book cannot be borrowed")
        void borrowBook_CannotBorrow_Returns422(String errorMessage) throws Exception {
            // Given
            when(inventoryService.borrowBook(VALID_USER_ID, VALID_BOOK_ID))
                .thenThrow(new BusinessRuleViolationException(errorMessage));

            // When & Then
            borrowBook(VALID_BOOK_ID)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value(errorMessage));

            verify(inventoryService).borrowBook(VALID_USER_ID, VALID_BOOK_ID);
            verify(borrowRecordMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("Should return 400 when book ID is invalid")
        void borrowBook_InvalidBookId_Returns400() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/inventory/books/invalid/borrow")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(inventoryService, never()).borrowBook(any(), any());
            verify(borrowRecordMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("Return Book Tests")
    class ReturnBookTests {

        @Test
        @DisplayName("Should return 200 and updated borrow record when book is successfully returned")
        void returnBook_ValidRequest_Returns200AndBorrowRecord() throws Exception {
            // Given
            BorrowRecord returnedRecord = TestDataFactory.createReturnedBorrowRecord(activeBorrowRecord);
            BorrowRecordResponse returnedResponse = TestDataFactory.createReturnedBorrowRecordResponse(returnedRecord);

            when(inventoryService.returnBook(VALID_USER_ID, VALID_BOOK_ID))
                .thenReturn(returnedRecord);
            when(borrowRecordMapper.toResponse(returnedRecord))
                .thenReturn(returnedResponse);

            // When & Then
            returnBook(VALID_BOOK_ID)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(returnedRecord.getId()))
                    .andExpect(jsonPath("$.bookId").value(VALID_BOOK_ID))
                    .andExpect(jsonPath("$.status").value("RETURNED"))
                    .andExpect(jsonPath("$.returnDate").isNotEmpty())
                    .andExpect(jsonPath("$.returnDate").value(returnedRecord.getReturnDate().toString()));

            verify(inventoryService).returnBook(VALID_USER_ID, VALID_BOOK_ID);
            verify(borrowRecordMapper).toResponse(returnedRecord);
        }

        @Test
        @DisplayName("Should return 404 when book is not found")
        void returnBook_BookNotFound_Returns404() throws Exception {
            // Given
            when(inventoryService.returnBook(VALID_USER_ID, INVALID_BOOK_ID))
                .thenThrow(new ResourceNotFoundException(BOOK_NOT_FOUND));

            // When & Then
            returnBook(INVALID_BOOK_ID)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(BOOK_NOT_FOUND));

            verify(inventoryService).returnBook(VALID_USER_ID, INVALID_BOOK_ID);
            verify(borrowRecordMapper, never()).toResponse(any());
        }

        @ParameterizedTest
        @ValueSource(strings = {RETURNED_OR_NOT_BORROWED, ALL_COPIES_AVAILABLE})
        @DisplayName("Should return 422 when book cannot be returned")
        void returnBook_CannotReturn_Returns422(String errorMessage) throws Exception {
            // Given
            when(inventoryService.returnBook(VALID_USER_ID, VALID_BOOK_ID))
                .thenThrow(new BusinessRuleViolationException(errorMessage));

            // When & Then
            returnBook(VALID_BOOK_ID)
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value(errorMessage));

            verify(inventoryService).returnBook(VALID_USER_ID, VALID_BOOK_ID);
            verify(borrowRecordMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("Should return 400 when book ID is invalid")
        void returnBook_InvalidBookId_Returns400() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/inventory/books/invalid/return")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(inventoryService, never()).returnBook(any(), any());
            verify(borrowRecordMapper, never()).toResponse(any());
        }
    }

    @Nested
    @DisplayName("Check Borrow Status Tests")
    class CheckBorrowStatusTests {

        @Test
        @DisplayName("Should return 200 and borrow status when checking valid book")
        void checkBorrowStatus_ValidBookId_Returns200AndStatus() throws Exception {
            // Given
            when(inventoryService.hasUserBorrowedBook(VALID_USER_ID, VALID_BOOK_ID))
                .thenReturn(true);

            // When & Then
            checkBorrowStatus(VALID_BOOK_ID)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.borrowed").value(true));

            verify(inventoryService).hasUserBorrowedBook(VALID_USER_ID, VALID_BOOK_ID);
        }

        @Test
        @DisplayName("Should return 404 when book is not found")
        void checkBorrowStatus_BookNotFound_Returns404() throws Exception {
            // Given
            when(inventoryService.hasUserBorrowedBook(VALID_USER_ID, INVALID_BOOK_ID))
                .thenThrow(new ResourceNotFoundException(BOOK_NOT_FOUND));

            // When & Then
            checkBorrowStatus(INVALID_BOOK_ID)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(BOOK_NOT_FOUND));

            verify(inventoryService).hasUserBorrowedBook(VALID_USER_ID, INVALID_BOOK_ID);
        }

        @Test
        @DisplayName("Should return 404 when user is not found")
        void checkBorrowStatus_UserNotFound_Returns404() throws Exception {
            // Given
            when(inventoryService.hasUserBorrowedBook(anyLong(), eq(VALID_BOOK_ID)))
                .thenThrow(new ResourceNotFoundException(USER_NOT_FOUND));

            // When & Then
            checkBorrowStatus(VALID_BOOK_ID)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(USER_NOT_FOUND));

            verify(inventoryService).hasUserBorrowedBook(anyLong(), eq(VALID_BOOK_ID));
        }

        @Test
        @DisplayName("Should return 400 when book ID is invalid")
        void checkBorrowStatus_InvalidBookId_Returns400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/inventory/books/invalid/status")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(inventoryService, never()).hasUserBorrowedBook(any(), any());
        }
    }

    @Nested
    @DisplayName("Get User Borrow Records Tests")
    class GetUserBorrowRecordsTests {

        @Test
        @DisplayName("Should return 200 and page of all borrow records")
        void getUserBorrowRecords_ValidRequest_Returns200AndPage() throws Exception {
            // Given
            List<Book> books = TestDataFactory.createBooksForPagination(3);
            Page<BorrowRecord> recordsPage = TestDataFactory.createDefaultBorrowRecordPage(testUser, books);
            Page<BorrowRecordResponse> responsePage = TestDataFactory.createDefaultBorrowRecordResponsePage(testUser, books);

            when(inventoryService.getUserBorrowRecordsByStatus(anyLong(), eq(BorrowStatus.BORROWED), any(Pageable.class)))
                .thenReturn(recordsPage);
            when(borrowRecordMapper.toResponse(any(BorrowRecord.class)))
                .thenAnswer(inv -> {
                    BorrowRecord record = inv.getArgument(0);
                    return responsePage.getContent().stream()
                        .filter(resp -> resp.getId().equals(record.getId()))
                        .findFirst()
                        .orElse(responsePage.getContent().get(0));
                });

            // When & Then
            getUserBorrowRecords()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(3))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.number").value(0))
                    .andExpect(jsonPath("$.size").value(10));

            verify(inventoryService).getUserBorrowRecordsByStatus(anyLong(), eq(BorrowStatus.BORROWED), any(Pageable.class));
            verify(borrowRecordMapper, times(recordsPage.getContent().size())).toResponse(any());
        }

        @Test
        @DisplayName("getUserBorrowRecords_shouldReturnPageOfBorrowRecordResponses_whenPageAndSizeProvided")
        void getUserBorrowRecords_shouldReturnPageOfBorrowRecordResponses_whenPageAndSizeProvided() throws Exception {
            // Given
            Pageable pageable = TestDataFactory.createPageable(1, 15);
             List<Book> books = TestDataFactory.createBooksForPagination(3);
            Page<BorrowRecord> recordsPage = TestDataFactory.createCustomBorrowRecordPage(testUser, books, pageable.getPageNumber(), pageable.getPageSize());
            Page<BorrowRecordResponse> responsePage = TestDataFactory.createCustomBorrowRecordResponsePage(testUser, books, pageable.getPageNumber(), pageable.getPageSize());
            
            when(inventoryService.getUserBorrowRecordsByStatus(anyLong(), eq(BorrowStatus.BORROWED), eq(pageable)))
                    .thenReturn(recordsPage);

             when(borrowRecordMapper.toResponse(any(BorrowRecord.class)))
            .thenAnswer(invocation -> {
                BorrowRecord record = invocation.getArgument(0);
                return responsePage.getContent().stream()
                        .filter(resp -> resp.getId().equals(record.getId()))
                        .findFirst()
                        .orElse(responsePage.getContent().get(0));
            });

            // When & Then
            getUserBorrowRecordsWithPagination(pageable.getPageNumber(), pageable.getPageSize())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(3))
                    .andExpect(jsonPath("$.number").value(pageable.getPageNumber()))
                    .andExpect(jsonPath("$.size").value(pageable.getPageSize()));

            verify(inventoryService).getUserBorrowRecordsByStatus(anyLong(), eq(BorrowStatus.BORROWED), eq(pageable));
            verify(borrowRecordMapper, times(responsePage.getNumberOfElements())).toResponse(any(BorrowRecord.class));
        }

        @Test
        @DisplayName("Should return 200 and page of borrow records filtered by status")
        void getUserBorrowRecords_FilteredByStatus_Returns200AndPage() throws Exception {
            // Given
            List<Book> books = TestDataFactory.createBooksForPagination(2);
            Page<BorrowRecord> recordsPage = TestDataFactory.createDefaultBorrowRecordPage(testUser, books);

            recordsPage.getContent().forEach(record -> {
                record.setStatus(BorrowStatus.RETURNED);
                record.setReturnDate(LocalDate.now().minusDays(2));
            });

            Page<BorrowRecordResponse> responsePage = new PageImpl<BorrowRecordResponse>(
                recordsPage.getContent().stream()
                    .map(record -> TestDataFactory.createBorrowRecordResponse(record))
                    .toList()
            );

            when(inventoryService.getUserBorrowRecordsByStatus(
                    eq(VALID_USER_ID), eq(BorrowStatus.RETURNED), any(Pageable.class)))
                .thenReturn(recordsPage);
            when(borrowRecordMapper.toResponse(any(BorrowRecord.class)))
                .thenAnswer(inv -> {
                    BorrowRecord record = inv.getArgument(0);
                    return responsePage.getContent().stream()
                        .filter(resp -> resp.getId().equals(record.getId()))
                        .findFirst()
                        .orElse(responsePage.getContent().get(0));
                });

            // When & Then
            getUserBorrowRecordsByStatus("RETURNED")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].status").value("RETURNED"))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.number").value(0))
                    .andExpect(jsonPath("$.size").value(10));

            verify(inventoryService).getUserBorrowRecordsByStatus(
                eq(VALID_USER_ID), eq(BorrowStatus.RETURNED), any(Pageable.class));
            verify(borrowRecordMapper, times(recordsPage.getContent().size())).toResponse(any());
        }

        @Test
        @DisplayName("Should return 400 when status is invalid")
        void getUserBorrowRecords_InvalidStatus_Returns400() throws Exception {
            // When & Then
            getUserBorrowRecordsByStatus("INVALID_STATUS")
                    .andExpect(status().isBadRequest());

            verify(inventoryService, never()).getUserBorrowRecordsByStatus(anyLong(), any(BorrowStatus.class), any(Pageable.class));
            verify(borrowRecordMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("Should return 404 when user is not found")
        void getUserBorrowRecords_UserNotFound_Returns404() throws Exception {
            // Given
            when(inventoryService.getUserBorrowRecordsByStatus(anyLong(), eq(BorrowStatus.BORROWED), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException(USER_NOT_FOUND));

            // When & Then
            getUserBorrowRecords()
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(USER_NOT_FOUND));

            verify(inventoryService).getUserBorrowRecordsByStatus(anyLong(), eq(BorrowStatus.BORROWED), any(Pageable.class));
            verify(borrowRecordMapper, never()).toResponse(any());
        }
    }
}