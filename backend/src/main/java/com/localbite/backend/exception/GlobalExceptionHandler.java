package com.localbite.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all REST controllers.
 * This catches exceptions and returns proper JSON error responses to the frontend.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle validation errors (e.g., invalid email format, missing required fields)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();

        // Collect all field validation errors
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        errorResponse.put("error", "Validation failed");
        errorResponse.put("message", "Please check the provided data");
        errorResponse.put("fieldErrors", fieldErrors);
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());

        log.warn("Validation error: {}", fieldErrors);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle authentication failures (wrong password, user not found)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        
        errorResponse.put("error", "Authentication failed");
        errorResponse.put("message", "Invalid email or password");
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.UNAUTHORIZED.value());

        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handle runtime exceptions (authentication failures, validation errors, etc.)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        String message = ex.getMessage();
        
        // 🔒 SECURITY FIX: Use consistent status codes for authentication-related errors
        HttpStatus status;
        if (isAuthenticationRelatedError(message)) {
            // Always use 401 for authentication-related errors to prevent information disclosure
            status = HttpStatus.UNAUTHORIZED;
        } else {
            // For other runtime exceptions, determine appropriate status
            status = determineStatusCode(message);
        }
        
        errorResponse.put("error", "Operation failed");
        errorResponse.put("message", message);
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());

        log.error("Runtime exception: {}", message);
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handle unexpected errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        
        errorResponse.put("error", "Internal server error");
        errorResponse.put("message", "An unexpected error occurred. Please try again later.");
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

        log.error("Unexpected error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Check if error message is authentication-related to apply consistent status codes
     */
    private boolean isAuthenticationRelatedError(String message) {
        if (message == null) return false;
        
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("invalid email or password") ||
               lowerMessage.contains("authentication failed") ||
               lowerMessage.contains("invalid") && lowerMessage.contains("password") ||
               lowerMessage.contains("credentials");
    }

    /**
     * Helper method to determine appropriate HTTP status code based on error message
     * 🔒 SECURITY: Reduced information disclosure by using more generic status codes
     */
    private HttpStatus determineStatusCode(String message) {
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            
            // For non-authentication errors, still provide some meaningful status codes
            if (lowerMessage.contains("validation") || 
                lowerMessage.contains("required") ||
                lowerMessage.contains("invalid format")) {
                return HttpStatus.BAD_REQUEST; // 400
            }
            
            if (lowerMessage.contains("not found")) {
                return HttpStatus.NOT_FOUND; // 404
            }
        }
        
        return HttpStatus.BAD_REQUEST; // 400 default - more generic than before
    }
} 