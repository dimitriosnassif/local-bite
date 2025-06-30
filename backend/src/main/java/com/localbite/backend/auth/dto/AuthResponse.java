package com.localbite.backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for authentication responses.
 * This is what we send back to the frontend after successful login/registration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    // The JWT token that frontend will use for subsequent requests
    private String token;
    
    // Refresh token for getting new access tokens
    private String refreshToken;
    
    // Token type (always "Bearer" for JWT)
    private String tokenType = "Bearer";
    
    // How long the token is valid (in seconds)
    private Long expiresIn;
    
    // User information to display in the frontend
    private UserInfo user;
    
    /**
     * Nested class for user information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private String phoneNumber;
        private Boolean emailVerified;
        private String provider; // LOCAL, GOOGLE, etc.
        private List<String> roles; // ["ROLE_BUYER", "ROLE_SELLER"]
    }
} 