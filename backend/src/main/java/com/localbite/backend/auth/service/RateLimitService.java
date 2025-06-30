package com.localbite.backend.auth.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.localbite.backend.auth.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RateLimitProperties rateLimitProperties;
    
    // Cache to store rate limiting buckets per IP/user
    private Cache<String, Bucket> bucketCache;

    @PostConstruct
    public void initializeCache() {
        this.bucketCache = CacheBuilder.newBuilder()
                .maximumSize(rateLimitProperties.getCache().getMaximumSize())
                .expireAfterAccess(rateLimitProperties.getCache().getExpireAfterAccessMinutes(), TimeUnit.MINUTES)
                .build();
        
        log.info("✅ Rate limiting cache initialized with max size: {}, expiry: {} minutes", 
                rateLimitProperties.getCache().getMaximumSize(),
                rateLimitProperties.getCache().getExpireAfterAccessMinutes());
    }

    /**
     * Check if request is allowed based on rate limiting rules
     * 
     * @param request HTTP request
     * @param limitType Type of rate limit to apply
     * @return true if request is allowed, false if rate limited
     */
    public boolean isRequestAllowed(HttpServletRequest request, RateLimitType limitType) {
        String key = generateRateLimitKey(request, limitType);
        Bucket bucket = getBucket(key, limitType);
        
        boolean allowed = bucket.tryConsume(1);
        
        if (!allowed) {
            log.warn("🚫 Rate limit exceeded for {} - Key: {}, Limit Type: {}", 
                    getClientIdentifier(request), key, limitType);
        } else {
            log.debug("✅ Request allowed for {} - Key: {}, Remaining tokens: {}", 
                    getClientIdentifier(request), key, bucket.getAvailableTokens());
        }
        
        return allowed;
    }

    /**
     * Check rate limit with user-specific limits (for authenticated requests)
     * 
     * @param request HTTP request
     * @param userId User ID for user-specific limits
     * @param limitType Type of rate limit to apply
     * @return true if request is allowed, false if rate limited
     */
    public boolean isRequestAllowed(HttpServletRequest request, String userId, RateLimitType limitType) {
        String key = generateUserRateLimitKey(userId, limitType);
        Bucket bucket = getBucket(key, limitType);
        
        boolean allowed = bucket.tryConsume(1);
        
        if (!allowed) {
            log.warn("🚫 Rate limit exceeded for user {} from {} - Key: {}, Limit Type: {}", 
                    userId, getClientIdentifier(request), key, limitType);
        } else {
            log.debug("✅ Request allowed for user {} - Key: {}, Remaining tokens: {}", 
                    userId, key, bucket.getAvailableTokens());
        }
        
        return allowed;
    }

    /**
     * Get remaining requests for a specific key and limit type
     */
    public long getRemainingRequests(HttpServletRequest request, RateLimitType limitType) {
        String key = generateRateLimitKey(request, limitType);
        Bucket bucket = getBucket(key, limitType);
        return bucket.getAvailableTokens();
    }

    /**
     * Get time until bucket refill in seconds
     */
    public long getTimeUntilRefill(HttpServletRequest request, RateLimitType limitType) {
        String key = generateRateLimitKey(request, limitType);
        Bucket bucket = getBucket(key, limitType);
        
        // If bucket has tokens, no waiting time
        if (bucket.getAvailableTokens() > 0) {
            return 0;
        }
        
        // Calculate refill time based on limit type
        RateLimitProperties.EndpointLimit limit = getEndpointLimit(limitType);
        if (limit != null) {
            return limit.getRefillPeriodMinutes() * 60; // Convert to seconds
        }
        
        return rateLimitProperties.getGlobal().getRefillPeriodMinutes() * 60;
    }

    /**
     * Generate rate limit key for IP-based limiting
     */
    private String generateRateLimitKey(HttpServletRequest request, RateLimitType limitType) {
        String clientIp = getClientIpAddress(request);
        return String.format("%s:%s", limitType.name(), clientIp);
    }

    /**
     * Generate rate limit key for user-based limiting
     */
    private String generateUserRateLimitKey(String userId, RateLimitType limitType) {
        return String.format("%s:user:%s", limitType.name(), userId);
    }

    /**
     * Get or create a bucket for the given key and limit type
     */
    private Bucket getBucket(String key, RateLimitType limitType) {
        try {
            return bucketCache.get(key, () -> createBucket(limitType));
        } catch (Exception e) {
            log.error("Failed to get bucket for key: {} - {}", key, e.getMessage());
            // Return a new bucket as fallback
            return createBucket(limitType);
        }
    }

    /**
     * Create a new bucket with appropriate configuration for the limit type
     */
    private Bucket createBucket(RateLimitType limitType) {
        Bandwidth bandwidth = createBandwidth(limitType);
        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    /**
     * Create bandwidth configuration based on rate limit type
     */
    private Bandwidth createBandwidth(RateLimitType limitType) {
        switch (limitType) {
            case LOGIN:
                RateLimitProperties.EndpointLimit loginLimit = rateLimitProperties.getAuth().getLogin();
                return Bandwidth.classic(
                        loginLimit.getCapacity(),
                        Refill.intervally(loginLimit.getRefillTokens(), 
                                Duration.ofMinutes(loginLimit.getRefillPeriodMinutes()))
                );
                
            case REGISTER:
                RateLimitProperties.EndpointLimit registerLimit = rateLimitProperties.getAuth().getRegister();
                return Bandwidth.classic(
                        registerLimit.getCapacity(),
                        Refill.intervally(registerLimit.getRefillTokens(), 
                                Duration.ofMinutes(registerLimit.getRefillPeriodMinutes()))
                );
                
            case EMAIL_VERIFICATION:
                RateLimitProperties.EndpointLimit emailLimit = rateLimitProperties.getAuth().getEmailVerification();
                return Bandwidth.classic(
                        emailLimit.getCapacity(),
                        Refill.intervally(emailLimit.getRefillTokens(), 
                                Duration.ofMinutes(emailLimit.getRefillPeriodMinutes()))
                );
                
            case PASSWORD_RESET:
                RateLimitProperties.EndpointLimit passwordLimit = rateLimitProperties.getAuth().getPasswordReset();
                return Bandwidth.classic(
                        passwordLimit.getCapacity(),
                        Refill.intervally(passwordLimit.getRefillTokens(), 
                                Duration.ofMinutes(passwordLimit.getRefillPeriodMinutes()))
                );
                
            case ADMIN:
                return Bandwidth.classic(
                        rateLimitProperties.getAdmin().getCapacity(),
                        Refill.intervally(rateLimitProperties.getAdmin().getRefillTokens(), 
                                Duration.ofMinutes(rateLimitProperties.getAdmin().getRefillPeriodMinutes()))
                );
                
            case GLOBAL:
            default:
                return Bandwidth.classic(
                        rateLimitProperties.getGlobal().getCapacity(),
                        Refill.intervally(rateLimitProperties.getGlobal().getRefillTokens(), 
                                Duration.ofMinutes(rateLimitProperties.getGlobal().getRefillPeriodMinutes()))
                );
        }
    }

    /**
     * Get endpoint limit configuration for a specific limit type
     */
    private RateLimitProperties.EndpointLimit getEndpointLimit(RateLimitType limitType) {
        switch (limitType) {
            case LOGIN:
                return rateLimitProperties.getAuth().getLogin();
            case REGISTER:
                return rateLimitProperties.getAuth().getRegister();
            case EMAIL_VERIFICATION:
                return rateLimitProperties.getAuth().getEmailVerification();
            case PASSWORD_RESET:
                return rateLimitProperties.getAuth().getPasswordReset();
            default:
                return null;
        }
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // Handle common proxy headers
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For can contain multiple IPs, get the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        String xForwarded = request.getHeader("X-Forwarded");
        if (xForwarded != null && !xForwarded.isEmpty() && !"unknown".equalsIgnoreCase(xForwarded)) {
            return xForwarded;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Get client identifier for logging
     */
    private String getClientIdentifier(HttpServletRequest request) {
        return getClientIpAddress(request);
    }

    /**
     * Enum defining different types of rate limits
     */
    public enum RateLimitType {
        GLOBAL,
        LOGIN,
        REGISTER,
        EMAIL_VERIFICATION,
        PASSWORD_RESET,
        ADMIN
    }
} 