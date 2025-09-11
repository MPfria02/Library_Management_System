package com.librarymanager.backend.dto.response;

import com.librarymanager.backend.entity.UserRole;

/**
 * Data Transfer Object for authentication responses.
 * Contains JWT token and user information.
 * 
 * @author Marcel Pulido
 */
public class AuthResponse {
    
    private String token;
    private String type = "Bearer";
    private String email;
    private UserRole role;
    private Long expiresIn; // Token expiration time in seconds
    
    // Constructors
    public AuthResponse() {}
    
    public AuthResponse(String token, String email, UserRole role, Long expiresIn) {
        this.token = token;
        this.email = email;
        this.role = role;
        this.expiresIn = expiresIn;
    }
    
    // Static builder method for clean construction
    public static Builder builder() {
        return new Builder();
    }
    
    // Builder class
    public static class Builder {
        private String token;
        private String type = "Bearer";
        private String email;
        private UserRole role;
        private Long expiresIn;
        
        public Builder token(String token) {
            this.token = token;
            return this;
        }
        
        public Builder type(String type) {
            this.type = type;
            return this;
        }
        
        public Builder email(String email) {
            this.email = email;
            return this;
        }
        
        public Builder role(UserRole role) {
            this.role = role;
            return this;
        }
        
        public Builder expiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }
        
        public AuthResponse build() {
            AuthResponse response = new AuthResponse();
            response.token = this.token;
            response.type = this.type;
            response.email = this.email;
            response.role = this.role;
            response.expiresIn = this.expiresIn;
            return response;
        }
    }
    
    // Getters and Setters
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
    }
    
    public Long getExpiresIn() {
        return expiresIn;
    }
    
    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }
    
    @Override
    public String toString() {
        return "AuthResponse{" +
                "token='[PROTECTED]'" +
                ", type='" + type + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", expiresIn=" + expiresIn +
                '}';
    }
}