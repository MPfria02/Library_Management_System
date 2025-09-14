package com.librarymanager.backend.repository;

import com.librarymanager.backend.entity.User;
import com.librarymanager.backend.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Spring Data JPA automatically implements these based on method names
    
    /**
     * Find user by email (used for authentication)
     * @param email the user's email
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if user with email already exists (used for registration)
     * @param email the email to check
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);
    
    /**
     * Find users by role
     * @param role the user role to search for
     * @return list of users with the specified role
     */
    List<User> findByRole(UserRole role);
    
    /**
     * Custom query to get all members ordered by name
     * Demonstrates JPQL (Java Persistence Query Language)
     * @return list of member users ordered by last name, then first name
     */
    @Query("SELECT u FROM User u WHERE u.role = 'MEMBER' ORDER BY u.lastName, u.firstName")
    List<User> findAllMembersOrderedByName();
    
    /**
     * Find users by partial name match (case-insensitive)
     * Useful for admin user search functionality
     * @param firstName partial first name
     * @param lastName partial last name
     * @return list of matching users
     */
    @Query("""
        SELECT u FROM User u
        WHERE (:firstName IS NOT NULL AND :firstName <> '' AND LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%')))
        OR (:lastName IS NOT NULL AND :lastName <> '' AND LOWER(u.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')))
    """)

    List<User> findByNameContainingIgnoreCase(String firstName, String lastName);
}