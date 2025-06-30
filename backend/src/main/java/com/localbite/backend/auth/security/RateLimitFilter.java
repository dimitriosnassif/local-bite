package com.localbite.backend.auth.security;

import com.localbite.backend.auth.service.RateLimitService;
import com.localbite.backend.auth.service.RateLimitService.RateLimitType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1) // Execute early in the filter chain
public class RateLimitFilter implements Filter {

    private final RateLimitService rateLimitService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        // Skip rate limiting for certain paths
        if (shouldSkipRateLimit(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        // Determine rate limit type based on endpoint
        RateLimitType limitType = determineRateLimitType(requestURI, method);
        
        if (limitType == null) {
            // No specific rate limit, apply global rate limit
            limitType = RateLimitType.GLOBAL;
        }

        // Check rate limit
        boolean allowed = checkRateLimit(httpRequest, limitType);

        if (!allowed) {
            // Rate limit exceeded - return 429 Too Many Requests
            handleRateLimitExceeded(httpRequest, httpResponse, limitType);
            return;
        }

        // Add rate limit headers to response
        addRateLimitHeaders(httpRequest, httpResponse, limitType);

        // Continue with the request
        chain.doFilter(request, response);
    }

    /**
     * Check if rate limiting should be skipped for this path
     */
    private boolean shouldSkipRateLimit(String requestURI) {
        // Skip rate limiting for:
        // - Static resources
        // - Health checks
        // - Swagger documentation
        // - Non-API endpoints
        return requestURI.startsWith("/static/") ||
               requestURI.startsWith("/css/") ||
               requestURI.startsWith("/js/") ||
               requestURI.startsWith("/images/") ||
               requestURI.equals("/actuator/health") ||
               requestURI.startsWith("/v3/api-docs") ||
               requestURI.startsWith("/swagger-ui") ||
               requestURI.equals("/favicon.ico");
    }

    /**
     * Determine the appropriate rate limit type based on the request URI and method
     */
    private RateLimitType determineRateLimitType(String requestURI, String method) {
        // Authentication endpoints
        if ("POST".equals(method)) {
            if (requestURI.equals("/api/auth/login")) {
                return RateLimitType.LOGIN;
            } else if (requestURI.equals("/api/auth/register")) {
                return RateLimitType.REGISTER;
            } else if (requestURI.contains("/verify-email") || 
                      requestURI.contains("/resend-verification") ||
                      requestURI.contains("/manual-verify")) {
                return RateLimitType.EMAIL_VERIFICATION;
            } else if (requestURI.contains("/password-reset") ||
                      requestURI.contains("/forgot-password") ||
                      requestURI.contains("/reset-password")) {
                return RateLimitType.PASSWORD_RESET;
            }
        }

        // Admin endpoints
        if (requestURI.startsWith("/api/auth/admin/") || 
            requestURI.startsWith("/api/admin/")) {
            return RateLimitType.ADMIN;
        }

        // Default to global rate limiting for other API endpoints
        if (requestURI.startsWith("/api/")) {
            return RateLimitType.GLOBAL;
        }

        return null; // No rate limiting for non-API endpoints
    }

    /**
     * Check rate limit for the request
     */
    private boolean checkRateLimit(HttpServletRequest request, RateLimitType limitType) {
        // For admin endpoints, also check user-specific rate limits if authenticated
        if (limitType == RateLimitType.ADMIN) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                String userId = auth.getName();
                return rateLimitService.isRequestAllowed(request, userId, limitType);
            }
        }

        // For authentication endpoints, also check user-specific limits for login attempts
        if (limitType == RateLimitType.LOGIN) {
            // Extract email from request body for user-specific rate limiting
            // This would require reading the request body, which is complex in a filter
            // For now, we'll use IP-based rate limiting
            return rateLimitService.isRequestAllowed(request, limitType);
        }

        // Default IP-based rate limiting
        return rateLimitService.isRequestAllowed(request, limitType);
    }

    /**
     * Handle rate limit exceeded - return 429 response
     */
    private void handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response, 
                                       RateLimitType limitType) throws IOException {
        
        long retryAfter = rateLimitService.getTimeUntilRefill(request, limitType);
        
        // Set response status and headers
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setHeader("X-RateLimit-Limit", getRateLimitInfo(limitType, "capacity"));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + (retryAfter * 1000)));

        // Create error response body
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Rate limit exceeded");
        errorResponse.put("message", "Too many requests. Please try again later.");
        errorResponse.put("limitType", limitType.name());
        errorResponse.put("retryAfter", retryAfter);
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        errorResponse.put("path", request.getRequestURI());

        // Write JSON response
        String jsonResponse = convertToJson(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();

        log.warn("🚫 Rate limit exceeded for {} - URI: {}, Limit Type: {}, Retry After: {}s", 
                getClientIpAddress(request), request.getRequestURI(), limitType, retryAfter);
    }

    /**
     * Add rate limit headers to successful responses
     */
    private void addRateLimitHeaders(HttpServletRequest request, HttpServletResponse response, 
                                   RateLimitType limitType) {
        long remaining = rateLimitService.getRemainingRequests(request, limitType);
        long resetTime = System.currentTimeMillis() + 60000; // Approximate reset time
        
        response.setHeader("X-RateLimit-Limit", getRateLimitInfo(limitType, "capacity"));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime));
        response.setHeader("X-RateLimit-Policy", limitType.name());
    }

    /**
     * Get rate limit information for headers
     */
    private String getRateLimitInfo(RateLimitType limitType, String info) {
        // This is a simplified version - in a real implementation,
        // you'd want to access the actual configuration values
        switch (limitType) {
            case LOGIN:
                return "capacity".equals(info) ? "5" : "5";
            case REGISTER:
                return "capacity".equals(info) ? "3" : "10";
            case EMAIL_VERIFICATION:
                return "capacity".equals(info) ? "3" : "15";
            case PASSWORD_RESET:
                return "capacity".equals(info) ? "2" : "30";
            case ADMIN:
                return "capacity".equals(info) ? "50" : "1";
            case GLOBAL:
            default:
                return "capacity".equals(info) ? "100" : "1";
        }
    }

    /**
     * Convert map to JSON string (simple implementation)
     */
    private String convertToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else if (value instanceof Number) {
                json.append(value);
            } else {
                json.append("\"").append(value.toString()).append("\"");
            }
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
} 