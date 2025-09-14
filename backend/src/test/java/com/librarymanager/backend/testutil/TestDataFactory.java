package com.librarymanager.backend.testutil;

import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.entity.User;
import com.librarymanager.backend.entity.UserRole;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Factory class for creating test data objects.
 * 
 * This utility class provides consistent test data creation across all tests,
 * following the builder pattern used in your entities and maintaining
 * realistic data for testing scenarios.
 * 
 * Benefits:
 * - Consistent test data across all test classes
 * - Easy maintenance when entity structure changes
 * - Realistic data for better test scenarios
 * - Reduces code duplication in tests
 * 
 * @author Marcel Pulido
 */
public class TestDataFactory {

    // ========== User Test Data Factory Methods ==========

    /**
     * Creates a default member user for testing.
     * 
     * @return User with MEMBER role and default test data
     */
    public static User createDefaultMemberUser() {
        return User.builder()
            .email("member@test.com")
            .password("hashedPassword123")
            .firstName("Test")
            .lastName("Member")
            .phone("1234567890")
            .role(UserRole.MEMBER)
            .build();
    }

    /**
     * Creates a default admin user for testing.
     * 
     * @return User with ADMIN role and default test data
     */
    public static User createDefaultAdminUser() {
        return User.builder()
            .email("admin@test.com")
            .password("adminPassword123")
            .firstName("Test")
            .lastName("Admin")
            .phone("0987654321")
            .role(UserRole.ADMIN)
            .build();
    }

    /**
     * Creates a custom user with specified parameters.
     * 
     * @param email user email
     * @param firstName first name
     * @param lastName last name
     * @param role user role
     * @return User with custom data
     */
    public static User createCustomUser(String email, String password, String firstName, String lastName, UserRole role) {
        return User.builder()
            .email(email)
            .password(password)
            .firstName(firstName)
            .lastName(lastName)
            .phone("5555555555")
            .role(role)
            .build();
    }

    /**
     * Creates a list of sample users for bulk testing.
     * 
     * @return List of diverse users for testing
     */
    public static List<User> createSampleUsers() {
        return Arrays.asList(
            createCustomUser("john.doe@test.com", "hashedPassword123", "John", "Doe", UserRole.MEMBER),
            createCustomUser("jane.smith@test.com", "hashedPassword456", "Jane", "Smith", UserRole.MEMBER),
            createCustomUser("admin.user@test.com", "adminPassword789", "Admin", "User", UserRole.ADMIN),
            createCustomUser("bob.wilson@test.com", "hashedPassword101", "Bob", "Wilson", UserRole.MEMBER)
        );
    }

    // ========== Book Test Data Factory Methods ==========

    /**
     * Creates a default available technology book.
     * 
     * @return Book with technology genre and available copies
     */
    public static Book createDefaultTechBook() {
        return Book.builder()
            .isbn("978-0134685991")
            .title("Effective Java")
            .author("Joshua Bloch")
            .description("Best practices for Java programming language")
            .genre(BookGenre.TECHNOLOGY)
            .totalCopies(5)
            .availableCopies(3)
            .publicationDate(LocalDate.of(2017, 12, 27))
            .build();
    }

    /**
     * Creates a default fiction book.
     * 
     * @return Book with fiction genre
     */
    public static Book createDefaultFictionBook() {
        return Book.builder()
            .isbn("978-0061120084")
            .title("To Kill a Mockingbird")
            .author("Harper Lee")
            .description("Classic American literature")
            .genre(BookGenre.FICTION)
            .totalCopies(4)
            .availableCopies(2)
            .publicationDate(LocalDate.of(1960, 7, 11))
            .build();
    }

    /**
     * Creates an unavailable book (all copies borrowed).
     * 
     * @return Book with zero available copies
     */
    public static Book createUnavailableBook() {
        return Book.builder()
            .isbn("978-0321356680")
            .title("Clean Code")
            .author("Robert Martin")
            .description("Writing clean, maintainable code")
            .genre(BookGenre.TECHNOLOGY)
            .totalCopies(3)
            .availableCopies(0) // All borrowed
            .publicationDate(LocalDate.of(2008, 8, 1))
            .build();
    }

    /**
     * Creates a custom book with specified parameters.
     * 
     * @param isbn book ISBN
     * @param title book title
     * @param author book author
     * @param genre book genre
     * @param totalCopies total number of copies
     * @param availableCopies available copies
     * @return Book with custom data
     */
    public static Book createCustomBook(String isbn, String title, String author,
                                        String description, LocalDate publicationDate,
                                        BookGenre genre, Integer totalCopies, Integer availableCopies) {
        return Book.builder()
            .isbn(isbn)
            .title(title)
            .author(author)
            .description(description)
            .genre(genre)
            .totalCopies(totalCopies)
            .availableCopies(availableCopies)
            .publicationDate(publicationDate)
            .build();
    }

    /**
     * Creates a list of diverse books for testing different scenarios.
     * 
     * @return List of books with various genres and availability
     */
    public static List<Book> createSampleBooks() {
        return Arrays.asList(
            createDefaultTechBook(),
            createDefaultFictionBook(),
            createUnavailableBook(),
            createCustomBook("978-0596009258", "Learning Python", "Mark Lutz", "Comprehensive guide to Python programming", LocalDate.of(2013, 6, 12),
                           BookGenre.TECHNOLOGY, 4, 4),
            createCustomBook("978-0307389732", "A Brief History of Time", "Stephen Hawking", "Exploration of the universe", LocalDate.of(1998, 9, 1),
                           BookGenre.SCIENCE, 3, 1),
            createCustomBook("978-0142000670", "Of Mice and Men", "John Steinbeck", "Classic American novel", LocalDate.of(1937, 4, 6),
                           BookGenre.FICTION, 2, 2),
            createCustomBook("978-0553213706", "Foundation", "Isaac Asimov", "Science fiction masterpiece", LocalDate.of(1951, 6, 1),
                           BookGenre.FANTASY, 5, 0)
        );
    }

    // ========== Specialized Test Data Methods ==========

    /**
     * Creates a book specifically for borrowing tests.
     * 
     * @return Book with sufficient available copies for borrowing
     */
    public static Book createBookForBorrowing() {
        return createCustomBook("978-1111111111", "Borrowing Test Book", "Test Author", "Test description", LocalDate.of(2020, 1, 1),
                              BookGenre.TECHNOLOGY, 10, 8);
    }

    /**
     * Creates a book specifically for return tests.
     * 
     * @return Book with some borrowed copies for return testing
     */
    public static Book createBookForReturning() {
        return createCustomBook("978-2222222222", "Return Test Book", "Test Author", "Test description", LocalDate.of(2020, 1, 1),
                              BookGenre.SCIENCE, 5, 2); // 3 copies borrowed
    }

    /**
     * Creates books for pagination testing.
     * 
     * @param count number of books to create
     * @return List of books for pagination tests
     */
    public static List<Book> createBooksForPagination(int count) {
        return Arrays.asList(
            createCustomBook("978-3333333333", "Book A", "Author A", "Description A", LocalDate.of(2020, 1, 1), BookGenre.FICTION, 1, 1),
            createCustomBook("978-4444444444", "Book B", "Author B", "Description B", LocalDate.of(2021, 1, 1), BookGenre.SCIENCE, 1, 1),
            createCustomBook("978-5555555555", "Book C", "Author C", "Description C", LocalDate.of(2022, 1, 1), BookGenre.TECHNOLOGY, 1, 0),
            createCustomBook("978-6666666666", "Book D", "Author D", "Description D", LocalDate.of(2023, 1, 1), BookGenre.HISTORY, 1, 1),
            createCustomBook("978-7777777777", "Book E", "Author E", "Description E", LocalDate.of(2024, 1, 1), BookGenre.MYSTERY, 1, 1)
        ).subList(0, Math.min(count, 5));
    }

    // ========== Genre-Specific Test Data ==========

    /**
     * Creates books for each genre to test genre-based operations.
     * 
     * @return List with one book per genre
     */
    public static List<Book> createBooksForAllGenres() {
        return Arrays.asList(
            createCustomBook("978-1000000001", "Fiction Test", "Fiction Author","Fiction Description" ,LocalDate.of(2020, 1, 1),
                           BookGenre.FICTION, 2, 1),
            createCustomBook("978-1000000002", "Non-Fiction Test", "Non-Fiction Author", "Non-Fiction Description", LocalDate.of(2020, 1, 1),
                           BookGenre.NON_FICTION, 2, 2),
            createCustomBook("978-1000000003", "Science Test", "Science Author", "Science Description", LocalDate.of(2020, 1, 1),
                           BookGenre.SCIENCE, 2, 0),
            createCustomBook("978-1000000004", "Technology Test", "Tech Author", "Tech Description", LocalDate.of(2020, 1, 1),
                           BookGenre.TECHNOLOGY, 2, 1),
            createCustomBook("978-1000000005", "History Test", "History Author", "History Description", LocalDate.of(2020, 1, 1),
                           BookGenre.HISTORY, 2, 2),
            createCustomBook("978-1000000006", "Biography Test", "Biography Author", "Biography Description", LocalDate.of(2020, 1, 1),
                           BookGenre.BIOGRAPHY, 2, 1),
            createCustomBook("978-1000000007", "Mystery Test", "Mystery Author", "Mystery Description", LocalDate.of(2020, 1, 1),
                           BookGenre.MYSTERY, 2, 0),
            createCustomBook("978-1000000008", "Romance Test", "Romance Author", "Romance Description", LocalDate.of(2020, 1, 1),
                           BookGenre.ROMANCE, 2, 2),
            createCustomBook("978-1000000009", "Fantasy Test", "Fantasy Author", "Fantasy Description", LocalDate.of(2020, 1, 1),
                           BookGenre.FANTASY, 2, 1)
        );
    }

    // ========== Validation Test Data ==========

    /**
     * Creates a book with invalid data for validation testing.
     * Note: This method creates data that should fail validation
     * 
     * @return Book with validation constraint violations
     */
    public static Book createInvalidBook() {
        return Book.builder()
            .isbn("") // Invalid: blank ISBN
            .title("") // Invalid: blank title
            .author("") // Invalid: blank author
            .genre(null) // Invalid: null genre
            .totalCopies(-1) // Invalid: negative total copies
            .availableCopies(10) // Invalid: more available than total
            .build();
    }

    /**
     * Creates a user with invalid data for validation testing.
     * 
     * @return User with validation constraint violations
     */
    public static User createInvalidUser() {
        return User.builder()
            .email("invalid-email") // Invalid: not a valid email format
            .password("") // Invalid: blank password
            .firstName("") // Invalid: blank first name
            .lastName("") // Invalid: blank last name
            .phone("12345678901234567890123456789") // Invalid: too long
            .build();
    }
}