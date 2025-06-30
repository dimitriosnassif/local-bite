package com.localbite.backend.auth.controller;

import com.localbite.backend.auth.annotation.RateLimit;
import com.localbite.backend.auth.dto.AuthResponse;
import com.localbite.backend.auth.dto.LoginRequest;
import com.localbite.backend.auth.dto.RegisterRequest;
import com.localbite.backend.auth.entity.User;
import com.localbite.backend.auth.service.RateLimitService.RateLimitType;
import com.localbite.backend.auth.service.UserService;
import com.localbite.backend.auth.service.EmailVerificationService;
import com.localbite.backend.auth.service.PasswordPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for authentication endpoints.
 * This is what the frontend will call to register and login users.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AuthController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordPolicyService passwordPolicyService;

    /**
     * Register a new user account
     * 
     * Frontend calls: POST /api/auth/register
     * 
     * Request body example:
     * {
     *   "email": "john@example.com",
     *   "password": "mypassword123",
     *   "firstName": "John",
     *   "lastName": "Doe",
     *   "phoneNumber": "1234567890",
     *   "role": "BUYER"
     * }
     */
    @PostMapping("/register")
    @RateLimit(type = RateLimitType.REGISTER, message = "Too many registration attempts. Please try again later.")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for email: {}", request.getEmail());
        
        try {
            // Call our service to handle the business logic
            AuthResponse response = userService.registerUser(request);
            
            log.info("User registration successful for email: {} - verification required", request.getEmail());
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.warn("Registration failed for email: {} - {}", request.getEmail(), e.getMessage());
            throw e; // Let global exception handler deal with it
        }
    }

    /**
     * Login user and get JWT token
     * 
     * Frontend calls: POST /api/auth/login
     * 
     * Request body example:
     * {
     *   "email": "john@example.com",
     *   "password": "mypassword123"
     * }
     */
    @PostMapping("/login")
    @RateLimit(type = RateLimitType.LOGIN, message = "Too many login attempts. Please try again later.")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email: {}", request.getEmail());
        
        try {
            AuthResponse response = userService.loginUser(request);
            log.info("User login successful for email: {}", request.getEmail());
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.warn("Login failed for email: {} - {}", request.getEmail(), e.getMessage());
            throw e; // Let global exception handler deal with it
        }
    }

    /**
     * Verify user email using verification token
     * 
     * Frontend calls: POST /api/auth/verify-email?token=<verification-token>
     */
    @PostMapping("/verify-email")
    @RateLimit(type = RateLimitType.EMAIL_VERIFICATION, message = "Too many email verification attempts. Please try again later.")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam("token") String token) {
        log.info("Email verification request received for token: {}", token);
        
        try {
            boolean verified = emailVerificationService.verifyEmail(token);
            
            Map<String, Object> response = new HashMap<>();
            if (verified) {
                response.put("success", true);
                response.put("message", "Email verified successfully! You can now log in.");
                log.info("Email verification successful for token: {}", token);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Invalid or expired verification token.");
                log.warn("Email verification failed for token: {}", token);
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Email verification error for token {}: {}", token, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Verification failed. Please try again.");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Resend verification email to user
     * 
     * Frontend calls: POST /api/auth/resend-verification
     * 
     * Request body example:
     * {
     *   "email": "john@example.com"
     * }
     */
    @PostMapping("/resend-verification")
    @RateLimit(type = RateLimitType.EMAIL_VERIFICATION, message = "Too many verification email requests. Please try again later.")
    public ResponseEntity<Map<String, Object>> resendVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        log.info("Resend verification request received for email: {}", email);
        
        try {
            emailVerificationService.resendVerificationEmail(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Verification email sent successfully! Please check your email.");
            
            log.info("Verification email resent to: {}", email);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.warn("Resend verification failed for email: {} - {}", email, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Check email verification status
     * 
     * Frontend calls: GET /api/auth/verification-status?email=<email>
     */
    @GetMapping("/verification-status")
    public ResponseEntity<Map<String, Object>> checkVerificationStatus(@RequestParam("email") String email) {
        log.info("Verification status check for email: {}", email);
        
        try {
            boolean verified = emailVerificationService.isEmailVerified(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("email", email);
            response.put("verified", verified);
            response.put("message", verified ? "Email is verified" : "Email is not verified");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error checking verification status for email {}: {}", email, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to check verification status");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Manually verify email (for testing/admin purposes)
     * 
     * Frontend calls: POST /api/auth/manual-verify
     * 
     * Request body example:
     * {
     *   "email": "john@example.com"
     * }
     */
    @PostMapping("/manual-verify")
    @RateLimit(type = RateLimitType.EMAIL_VERIFICATION, message = "Too many manual verification attempts. Please try again later.")
    public ResponseEntity<Map<String, Object>> manualVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        log.info("Manual verification request for email: {}", email);
        
        try {
            emailVerificationService.manuallyVerifyEmail(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email verified manually");
            
            log.info("Email manually verified for: {}", email);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.warn("Manual verification failed for email: {} - {}", email, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get current user info (requires JWT token)
     * 
     * Frontend calls: GET /api/auth/me
     * Headers: Authorization: Bearer <jwt-token>
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserInfo> getCurrentUser() {
        try {
            // Get current authenticated user from SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();
            
            // Get user from database
            User user = userService.getUserByEmail(userEmail);
            
            // Build user info response
            AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .phoneNumber(user.getPhoneNumber())
                    .emailVerified(user.getEmailVerified())
                    .provider(user.getProvider().name())
                    .roles(user.getRoles().stream()
                            .map(role -> role.getName())
                            .toList())
                    .build();
            
            return ResponseEntity.ok(userInfo);
            
        } catch (Exception e) {
            log.error("Error getting current user info: {}", e.getMessage());
            throw new RuntimeException("Failed to get user information");
        }
    }

    /**
     * Health check endpoint for authentication service
     * 
     * Frontend calls: GET /api/auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Authentication Service");
        response.put("message", "LocalBite authentication is running!");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    // ==================== ADMINISTRATIVE ENDPOINTS ====================

    /**
     * Lock a user account (ADMIN only)
     * 
     * Frontend calls: POST /api/auth/admin/lock-account
     * Headers: Authorization: Bearer <admin-jwt-token>
     * 
     * Request body example:
     * {
     *   "email": "user@example.com",
     *   "reason": "Suspicious activity detected"
     * }
     */
    @PostMapping("/admin/lock-account")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(type = RateLimitType.ADMIN, perUser = true, message = "Too many admin requests. Please try again later.")
    public ResponseEntity<Map<String, Object>> lockAccount(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String reason = request.get("reason");
        log.info("Admin lock account request for email: {} - Reason: {}", email, reason);
        
        try {
            String message = userService.lockUserAccount(email, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", message);
            response.put("email", email);
            response.put("action", "LOCKED");
            response.put("reason", reason);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.warn("Admin lock account failed for email: {} - {}", email, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Unlock a user account (ADMIN only)
     * 
     * Frontend calls: POST /api/auth/admin/unlock-account
     * Headers: Authorization: Bearer <admin-jwt-token>
     * 
     * Request body example:
     * {
     *   "email": "user@example.com"
     * }
     */
    @PostMapping("/admin/unlock-account")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(type = RateLimitType.ADMIN, perUser = true, message = "Too many admin requests. Please try again later.")
    public ResponseEntity<Map<String, Object>> unlockAccount(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        log.info("Admin unlock account request for email: {}", email);
        
        try {
            String message = userService.unlockUserAccount(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", message);
            response.put("email", email);
            response.put("action", "UNLOCKED");
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.warn("Admin unlock account failed for email: {} - {}", email, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Enable a user account (ADMIN only)
     * 
     * Frontend calls: POST /api/auth/admin/enable-account
     * Headers: Authorization: Bearer <admin-jwt-token>
     * 
     * Request body example:
     * {
     *   "email": "user@example.com"
     * }
     */
    @PostMapping("/admin/enable-account")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(type = RateLimitType.ADMIN, perUser = true, message = "Too many admin requests. Please try again later.")
    public ResponseEntity<Map<String, Object>> enableAccount(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        log.info("Admin enable account request for email: {}", email);
        
        try {
            String message = userService.enableUserAccount(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", message);
            response.put("email", email);
            response.put("action", "ENABLED");
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.warn("Admin enable account failed for email: {} - {}", email, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Disable a user account (ADMIN only)
     * 
     * Frontend calls: POST /api/auth/admin/disable-account
     * Headers: Authorization: Bearer <admin-jwt-token>
     * 
     * Request body example:
     * {
     *   "email": "user@example.com",
     *   "reason": "Policy violation"
     * }
     */
    @PostMapping("/admin/disable-account")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(type = RateLimitType.ADMIN, perUser = true, message = "Too many admin requests. Please try again later.")
    public ResponseEntity<Map<String, Object>> disableAccount(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String reason = request.get("reason");
        log.info("Admin disable account request for email: {} - Reason: {}", email, reason);
        
        try {
            String message = userService.disableUserAccount(email, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", message);
            response.put("email", email);
            response.put("action", "DISABLED");
            response.put("reason", reason);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.warn("Admin disable account failed for email: {} - {}", email, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get user account status (ADMIN only)
     * 
     * Frontend calls: GET /api/auth/admin/account-status?email=<email>
     * Headers: Authorization: Bearer <admin-jwt-token>
     */
    @GetMapping("/admin/account-status")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(type = RateLimitType.ADMIN, perUser = true, message = "Too many admin requests. Please try again later.")
    public ResponseEntity<Map<String, Object>> getAccountStatus(@RequestParam("email") String email) {
        log.info("Admin account status check for email: {}", email);
        
        try {
            Map<String, Object> accountStatus = userService.getUserAccountStatus(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("accountStatus", accountStatus);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.warn("Admin account status check failed for email: {} - {}", email, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Reset failed login attempts for a user (ADMIN only)
     * 
     * Frontend calls: POST /api/auth/admin/reset-failed-attempts
     * Headers: Authorization: Bearer <admin-jwt-token>
     * 
     * Request body example:
     * {
     *   "email": "user@example.com"
     * }
     */
    @PostMapping("/admin/reset-failed-attempts")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(type = RateLimitType.ADMIN, perUser = true, message = "Too many admin requests. Please try again later.")
    public ResponseEntity<Map<String, Object>> resetFailedAttempts(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        log.info("Admin reset failed attempts request for email: {}", email);
        
        try {
            String message = userService.resetFailedLoginAttempts(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", message);
            response.put("email", email);
            response.put("action", "RESET_FAILED_ATTEMPTS");
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.warn("Admin reset failed attempts failed for email: {} - {}", email, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== PASSWORD POLICY ENDPOINTS ====================

    /**
     * Check password strength against current policy
     * 
     * Frontend calls: POST /api/auth/check-password-strength
     * 
     * Request body example:
     * {
     *   "password": "MySecurePassword123!",
     *   "email": "user@example.com",
     *   "firstName": "John",
     *   "lastName": "Doe"
     * }
     */
    @PostMapping("/check-password-strength")
    public ResponseEntity<Map<String, Object>> checkPasswordStrength(@RequestBody Map<String, String> request) {
        String password = request.get("password");
        String email = request.get("email");
        String firstName = request.get("firstName");
        String lastName = request.get("lastName");
        
        log.info("Password strength check requested");
        
        try {
            // Create temporary user for validation (if personal info provided)
            User tempUser = null;
            if (email != null || firstName != null || lastName != null) {
                tempUser = User.builder()
                        .email(email)
                        .firstName(firstName)
                        .lastName(lastName)
                        .build();
            }
            
            PasswordPolicyService.PasswordValidationResult result = 
                    passwordPolicyService.validatePassword(password, tempUser, null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", result.isValid());
            response.put("violations", result.getViolations());
            response.put("requirements", result.getRequirements());
            response.put("suggestions", passwordPolicyService.generatePasswordSuggestions());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Password strength check error: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to check password strength");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get current password policy requirements
     * 
     * Frontend calls: GET /api/auth/password-policy
     */
    @GetMapping("/password-policy")
    public ResponseEntity<Map<String, Object>> getPasswordPolicy() {
        log.info("Password policy information requested");
        
        try {
            List<String> suggestions = passwordPolicyService.generatePasswordSuggestions();
            
            Map<String, Object> response = new HashMap<>();
            response.put("requirements", suggestions);
            response.put("enforcementLevel", "STRICT"); // Could be made dynamic
            response.put("message", "Password must meet all security requirements");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Password policy retrieval error: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to retrieve password policy");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Check if user's password needs to be changed (expiry check)
     * 
     * Frontend calls: GET /api/auth/password-expiry-status
     * Headers: Authorization: Bearer <jwt-token>
     */
    @GetMapping("/password-expiry-status")
    public ResponseEntity<Map<String, Object>> checkPasswordExpiry() {
        log.info("Password expiry status check requested");
        
        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || 
                "anonymousUser".equals(authentication.getName())) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String userEmail = authentication.getName();
            User user = userService.getUserByEmail(userEmail);
            
            PasswordPolicyService.PasswordExpiryInfo expiryInfo = 
                    passwordPolicyService.checkPasswordExpiry(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("expired", expiryInfo.isExpired());
            response.put("warning", expiryInfo.isWarning());
            response.put("daysUntilExpiry", expiryInfo.getDaysUntilExpiry());
            
            if (expiryInfo.isExpired()) {
                response.put("message", "Your password has expired. Please change it immediately.");
                response.put("action", "CHANGE_PASSWORD_REQUIRED");
            } else if (expiryInfo.isWarning()) {
                response.put("message", "Your password will expire in " + expiryInfo.getDaysUntilExpiry() + " days.");
                response.put("action", "CHANGE_PASSWORD_RECOMMENDED");
            } else {
                response.put("message", "Your password is current.");
                response.put("action", "NO_ACTION_REQUIRED");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Password expiry check error: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to check password expiry");
            return ResponseEntity.badRequest().body(response);
        }
    }
} 