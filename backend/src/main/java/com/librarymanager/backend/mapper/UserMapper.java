package com.librarymanager.backend.mapper;

import com.librarymanager.backend.dto.request.UserRegistrationRequest;
import com.librarymanager.backend.dto.response.UserResponse;
import com.librarymanager.backend.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper class for converting between User entities and DTOs.
 * 
 * This class provides mapping methods to convert User entities to response DTOs
 * and registration requests to User entities. It encapsulates the conversion logic
 * and ensures consistent mapping across the application.
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
@Component
public class UserMapper {
    
    // Request DTO → Entity (for creating/updating)
    public User toEntity(UserRegistrationRequest request) {
        return User.builder()
            .email(request.getEmail())
            .password(request.getPassword())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phone(request.getPhone())
            .role(request.getRole())
            .build();
    }
    
    // Entity → Response DTO (for API responses)
    public UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getPhone(),
            user.getRole()
        );
    }
}
