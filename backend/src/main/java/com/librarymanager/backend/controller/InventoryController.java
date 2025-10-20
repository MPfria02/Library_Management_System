package com.librarymanager.backend.controller;

import com.librarymanager.backend.dto.response.BorrowRecordResponse;
import com.librarymanager.backend.entity.BorrowRecord;
import com.librarymanager.backend.entity.BorrowStatus;
import com.librarymanager.backend.mapper.BorrowRecordMapper;
import com.librarymanager.backend.security.CustomUserDetails;
import com.librarymanager.backend.service.InventoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * REST controller for book inventory operations.
 * 
 * Provides simple inventory management functionality for borrowing and returning books.
 * Designed for circulation management and book availability tracking.
 * 
 * Endpoints:
 * - POST /api/inventory/books/{id}/borrow - Borrow a book
 * - POST /api/inventory/books/{id}/return - Return a book
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
@RestController
@RequestMapping("/api/inventory/books")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {
    
    private final InventoryService inventoryService;
    private final BorrowRecordMapper borrowRecordMapper;

    /**
     * Borrow a book
     * 
     * @param bookId Book to borrow (from path)
     * @param userDetails Current user (from JWT via Spring Security)
     * @return Borrow record with book details and due date
     */
    @PostMapping("/{bookId}/borrow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BorrowRecordResponse> borrowBook(@PathVariable Long bookId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.debug("Borrow request: bookId={}, userId={}", bookId, userDetails.getId());
        
        BorrowRecord borrowRecord = inventoryService.borrowBook(
            userDetails.getId(), 
            bookId
        );
        
        return ResponseEntity.ok(borrowRecordMapper.toResponse(borrowRecord));
    }
    
    /**
     * Return a borrowed book
     * 
     * @param bookId Book to return (from path)
     * @param userDetails Current user (from JWT via Spring Security)
     * @return Updated borrow record with return date
     */
    @PostMapping("/{bookId}/return")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BorrowRecordResponse> returnBook(@PathVariable Long bookId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.debug("Return request: bookId={}, userId={}", bookId, userDetails.getId());
        
        BorrowRecord borrowRecord = inventoryService.returnBook(
            userDetails.getId(), 
            bookId
        );
        
        return ResponseEntity.ok(borrowRecordMapper.toResponse(borrowRecord));
    }
    
    /**
     * Check if current user has borrowed a specific book
     * Used by frontend to show correct button (Borrow vs Return)
     * 
     * @param bookId Book to check
     * @param userDetails Current user
     * @return { "borrowed": true/false }
     */
    @GetMapping("/{bookId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BorrowStatusResponse> checkBorrowStatus(@PathVariable Long bookId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean borrowed = inventoryService.hasUserBorrowedBook(
            userDetails.getId(), 
            bookId
        );
        
        return ResponseEntity.ok(new BorrowStatusResponse(borrowed));
    }
    
    // Simple response DTO for status check
    public record BorrowStatusResponse(boolean borrowed) {}

    /**
     * Get current user's borrow records with optional status filter
     * 
     * <p>Endpoint: GET /api/users/me/borrows</p>
     * <p>
     * Query params:
     * <ul>
     *    <li> page: Page number (0-indexed, default: 0)</li>
     *    <li> size: Page size (default: 10)</li>
     *    <li> status: Filter by BORROWED or RETURNED (optional)</li>
     * </ul>
     * </p>
     * <p>
     * Examples:
     * <ul>
     *  <li> GET /api/users/me/borrows → All borrows, page 0, size 10 </li>
     *  <li> GET /api/users/me/borrows?status=BORROWED → Active borrows only</li>
     *  <li> GET /api/users/me/borrows?status=RETURNED&page=1 → History, page 1</li>
     * </ul>
     * </p>
     * @param status Optional status filter
     * @param page Page number (default 0)
     * @param size Page size (default 10)
     * @param userDetails Current user (from JWT)
     * @return Paginated borrow records
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<BorrowRecordResponse>> getUserBorrowRecords(
        @RequestParam(required = false, defaultValue = "BORROWED") BorrowStatus status,
        @RequestParam(required = false,defaultValue = "0") int page,
        @RequestParam(required = false, defaultValue = "10") int size,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.debug("Fetching borrow records for user {} (status: {}, page: {}, size: {})", 
            userDetails.getId(), status, page, size);
        
        // Sort by due date ascending (closest due date first)
        Pageable pageable = PageRequest.of(page, size, Sort.by("dueDate").ascending());
        
        Page<BorrowRecord> borrowRecords = inventoryService.getUserBorrowRecordsByStatus(
                userDetails.getId(), 
                status, 
                pageable
            );
        
        // Map entities to DTOs
        Page<BorrowRecordResponse> response = borrowRecords.map(borrowRecordMapper::toResponse);
        
        log.debug("Returning {} borrow records for user {}", 
            response.getNumberOfElements(), userDetails.getId());
        
        return ResponseEntity.ok(response);
    }
}
