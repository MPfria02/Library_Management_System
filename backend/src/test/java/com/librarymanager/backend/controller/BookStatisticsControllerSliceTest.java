package com.librarymanager.backend.controller;

import com.librarymanager.backend.dto.response.BookResponse;
import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.mapper.BookMapper;
import com.librarymanager.backend.security.JwtAuthenticationFilter;
import com.librarymanager.backend.service.BookStatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for BookStatisticsController using @WebMvcTest.
 * 
 * Tests the web layer in isolation, focusing on HTTP behavior,
 * request/response mapping, and exception handling for statistics operations.
 * 
 * @author Marcel Pulido
 */
@WebMvcTest(BookStatisticsController.class)
@AutoConfigureMockMvc(addFilters = false)
public class BookStatisticsControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookStatisticsService bookStatisticsService;

    @MockitoBean
    private BookMapper bookMapper;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ========== Count Statistics Tests ==========

    @Test
    public void shouldReturn200WhenGettingTotalBookCount() throws Exception {
        // Arrange
        given(bookStatisticsService.countAllBooks()).willReturn(150L);

        // Act & Assert
        mockMvc.perform(get("/api/statistics/books/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(150));
    }

    @Test
    public void shouldReturn200WhenGettingAvailableBookCount() throws Exception {
        // Arrange
        given(bookStatisticsService.countAvailableBooks()).willReturn(120L);

        // Act & Assert
        mockMvc.perform(get("/api/statistics/books/available/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(120));
    }

    @Test
    public void shouldReturn200WhenGettingAvailableBooksCountByGenre() throws Exception {
        // Arrange
        given(bookStatisticsService.countAvailableBooksByGenre(BookGenre.FICTION)).willReturn(45L);

        // Act & Assert
        mockMvc.perform(get("/api/statistics/books/genre/FICTION/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(45));
    }

    @Test
    public void shouldReturn200WhenGettingAvailableBooksCountByScienceGenre() throws Exception {
        // Arrange
        given(bookStatisticsService.countAvailableBooksByGenre(BookGenre.SCIENCE)).willReturn(30L);

        // Act & Assert
        mockMvc.perform(get("/api/statistics/books/genre/SCIENCE/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(30));
    }

    @Test
    public void shouldReturn200WhenGettingZeroCountForEmptyGenre() throws Exception {
        // Arrange
        given(bookStatisticsService.countAvailableBooksByGenre(BookGenre.HISTORY)).willReturn(0L);

        // Act & Assert
        mockMvc.perform(get("/api/statistics/books/genre/HISTORY/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
    }

    // ========== Borrowed Books Tests ==========

    @Test
    public void shouldReturn200WhenGettingBooksWithBorrowedCopies() throws Exception {
        // Arrange
        Book book1 = new Book();
        book1.setId(1L);
        book1.setTitle("Borrowed Book 1");
        book1.setAuthor("Author 1");
        book1.setGenre(BookGenre.FICTION);
        book1.setAvailableCopies(1);
        book1.setPublicationDate(LocalDate.of(2023, 1, 1));
        
        Book book2 = new Book();
        book2.setId(2L);
        book2.setTitle("Borrowed Book 2");
        book2.setAuthor("Author 2");
        book2.setGenre(BookGenre.SCIENCE);
        book2.setAvailableCopies(0);
        book2.setPublicationDate(LocalDate.of(2023, 2, 1));
        
        List<Book> booksWithBorrowedCopies = Arrays.asList(book1, book2);
        
        BookResponse response1 = BookResponse.builder()
            .id(1L)
            .title("Borrowed Book 1")
            .author("Author 1")
            .genre(BookGenre.FICTION)
            .availableCopies(1)
            .publicationDate(LocalDate.of(2023, 1, 1))
            .build();
            
        BookResponse response2 = BookResponse.builder()
            .id(2L)
            .title("Borrowed Book 2")
            .author("Author 2")
            .genre(BookGenre.SCIENCE)
            .availableCopies(0)
            .publicationDate(LocalDate.of(2023, 2, 1))
            .build();

        given(bookStatisticsService.getBooksWithBorrowedCopies()).willReturn(booksWithBorrowedCopies);
        given(bookMapper.toResponse(book1)).willReturn(response1);
        given(bookMapper.toResponse(book2)).willReturn(response2);

        // Act & Assert
        mockMvc.perform(get("/api/statistics/books/borrowed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Borrowed Book 1"))
                .andExpect(jsonPath("$[0].availableCopies").value(1))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].title").value("Borrowed Book 2"))
                .andExpect(jsonPath("$[1].availableCopies").value(0));
    }

    @Test
    public void shouldReturn200WhenNoBooksHaveBorrowedCopies() throws Exception {
        // Arrange
        List<Book> emptyList = Arrays.asList();
        given(bookStatisticsService.getBooksWithBorrowedCopies()).willReturn(emptyList);

        // Act & Assert
        mockMvc.perform(get("/api/statistics/books/borrowed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ========== Availability Percentage Tests ==========

    @Test
    public void shouldReturn200WhenGettingAvailabilityPercentage() throws Exception {
        // Arrange
        given(bookStatisticsService.countAllBooks()).willReturn(100L);
        given(bookStatisticsService.countAvailableBooks()).willReturn(75L);

        // Act & Assert
        mockMvc.perform(get("/api/statistics/books/availability/percentage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(75.0));
    }

    @Test
    public void shouldReturn200WhenGettingFullAvailabilityPercentage() throws Exception {
        // Arrange
        given(bookStatisticsService.countAllBooks()).willReturn(50L);
        given(bookStatisticsService.countAvailableBooks()).willReturn(50L);

        // Act & Assert
        mockMvc.perform(get("/api/statistics/books/availability/percentage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(100.0));
    }

    @Test
    public void shouldReturn200WhenGettingZeroAvailabilityPercentage() throws Exception {
        // Arrange
        given(bookStatisticsService.countAllBooks()).willReturn(50L);
        given(bookStatisticsService.countAvailableBooks()).willReturn(0L);

        // Act & Assert
        mockMvc.perform(get("/api/statistics/books/availability/percentage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0.0));
    }

    @Test
    public void shouldReturn200WhenGettingRoundedAvailabilityPercentage() throws Exception {
        // Arrange
        given(bookStatisticsService.countAllBooks()).willReturn(100L);
        given(bookStatisticsService.countAvailableBooks()).willReturn(33L);

        // Act & Assert
        mockMvc.perform(get("/api/statistics/books/availability/percentage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(33.0));
    }

    @Test
    public void shouldReturn200WhenGettingAvailabilityPercentageWithZeroTotalBooks() throws Exception {
        // Arrange
        given(bookStatisticsService.countAllBooks()).willReturn(0L);
        given(bookStatisticsService.countAvailableBooks()).willReturn(0L);

        // Act & Assert
        mockMvc.perform(get("/api/statistics/books/availability/percentage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0.0));
    }

    @Test
    public void shouldReturn200WhenGettingComplexAvailabilityPercentage() throws Exception {
        // Arrange
        given(bookStatisticsService.countAllBooks()).willReturn(137L);
        given(bookStatisticsService.countAvailableBooks()).willReturn(89L);

        // Act & Assert
        mockMvc.perform(get("/api/statistics/books/availability/percentage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(64.96));
    }

    // ========== Edge Cases ==========

    @Test
    public void shouldReturn400WhenInvalidGenreIsProvided() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/statistics/books/genre/INVALID_GENRE/count"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
    }

    @Test
    public void shouldReturn200WhenGettingLargeCounts() throws Exception {
        // Arrange
        given(bookStatisticsService.countAllBooks()).willReturn(10000L);
        given(bookStatisticsService.countAvailableBooks()).willReturn(7500L);

        // Act & Assert
        mockMvc.perform(get("/api/statistics/books/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(10000));
                
        mockMvc.perform(get("/api/statistics/books/available/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(7500));
    }

    @Test
    public void shouldReturn200WhenGettingStatisticsWithManyBorrowedBooks() throws Exception {
        // Arrange
        Book book1 = new Book();
        book1.setId(1L);
        book1.setTitle("Book 1");
        book1.setAuthor("Author 1");
        book1.setGenre(BookGenre.FICTION);
        book1.setAvailableCopies(0);
        
        Book book2 = new Book();
        book2.setId(2L);
        book2.setTitle("Book 2");
        book2.setAuthor("Author 2");
        book2.setGenre(BookGenre.SCIENCE);
        book2.setAvailableCopies(1);
        
        Book book3 = new Book();
        book3.setId(3L);
        book3.setTitle("Book 3");
        book3.setAuthor("Author 3");
        book3.setGenre(BookGenre.HISTORY);
        book3.setAvailableCopies(0);
        
        List<Book> booksWithBorrowedCopies = Arrays.asList(book1, book2, book3);
        
        BookResponse response1 = BookResponse.builder()
            .id(1L)
            .title("Book 1")
            .author("Author 1")
            .genre(BookGenre.FICTION)
            .availableCopies(0)
            .build();
            
        BookResponse response2 = BookResponse.builder()
            .id(2L)
            .title("Book 2")
            .author("Author 2")
            .genre(BookGenre.SCIENCE)
            .availableCopies(1)
            .build();
            
        BookResponse response3 = BookResponse.builder()
            .id(3L)
            .title("Book 3")
            .author("Author 3")
            .genre(BookGenre.HISTORY)
            .availableCopies(0)
            .build();

        given(bookStatisticsService.getBooksWithBorrowedCopies()).willReturn(booksWithBorrowedCopies);
        given(bookMapper.toResponse(book1)).willReturn(response1);
        given(bookMapper.toResponse(book2)).willReturn(response2);
        given(bookMapper.toResponse(book3)).willReturn(response3);

        // Act & Assert
        mockMvc.perform(get("/api/statistics/books/borrowed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[2].id").value(3));
    }

    @Test
    public void shouldReturn200WhenGettingStatisticsForAllGenres() throws Exception {
        // Arrange
        given(bookStatisticsService.countAvailableBooksByGenre(BookGenre.FICTION)).willReturn(25L);
        given(bookStatisticsService.countAvailableBooksByGenre(BookGenre.SCIENCE)).willReturn(15L);
        given(bookStatisticsService.countAvailableBooksByGenre(BookGenre.HISTORY)).willReturn(10L);

        // Act & Assert
        mockMvc.perform(get("/api/statistics/books/genre/FICTION/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(25));
                
        mockMvc.perform(get("/api/statistics/books/genre/SCIENCE/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(15));
                
        mockMvc.perform(get("/api/statistics/books/genre/HISTORY/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(10));
    }

    @Test
    public void shouldReturn500WhenUnexpectedErrorOccurs() throws Exception {
        // Arrange
        given(bookStatisticsService.countAllBooks()).willThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        mockMvc.perform(get("/api/statistics/books/count"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"));
    }
}