package com.localbite.backend.auth.annotation;

import com.localbite.backend.auth.service.RateLimitService.RateLimitType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply rate limiting to specific controller methods or classes.
 * 
 * Usage examples:
 * 
 * Method level:
 * @RateLimit(type = RateLimitType.LOGIN)
 * public ResponseEntity<?> login(@RequestBody LoginRequest request) { ... }
 * 
 * Class level (applies to all methods):
 * @RateLimit(type = RateLimitType.ADMIN)
 * @RestController
 * public class AdminController { ... }
 * 
 * Custom limits:
 * @RateLimit(type = RateLimitType.GLOBAL, capacity = 10, refillTokens = 5, refillPeriodMinutes = 1)
 * public ResponseEntity<?> specialEndpoint() { ... }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * Type of rate limit to apply
     */
    RateLimitType type() default RateLimitType.GLOBAL;
    
    /**
     * Maximum number of requests in the bucket (bucket capacity)
     * If not specified, uses the default from configuration
     */
    int capacity() default -1;
    
    /**
     * Number of tokens to refill
     * If not specified, uses the default from configuration
     */
    int refillTokens() default -1;
    
    /**
     * Refill period in minutes
     * If not specified, uses the default from configuration
     */
    int refillPeriodMinutes() default -1;
    
    /**
     * Whether to use user-specific rate limiting for authenticated users
     * If true, rate limits will be applied per user instead of per IP
     */
    boolean perUser() default false;
    
    /**
     * Custom error message when rate limit is exceeded
     */
    String message() default "Rate limit exceeded. Please try again later.";
    
    /**
     * Whether to skip rate limiting for this endpoint
     * Useful for temporarily disabling rate limiting during testing
     */
    boolean disabled() default false;
} 