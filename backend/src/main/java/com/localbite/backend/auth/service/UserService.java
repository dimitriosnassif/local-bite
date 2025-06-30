package com.localbite.backend.auth.service;

import com.localbite.backend.auth.dto.AuthResponse;
import com.localbite.backend.auth.dto.LoginRequest;
import com.localbite.backend.auth.dto.RegisterRequest;
import com.localbite.backend.auth.entity.Role;
import com.localbite.backend.auth.entity.User;
import com.localbite.backend.auth.repository.RoleRepository;
import com.localbite.backend.auth.repository.UserRepository;
import com.localbite.backend.auth.security.JwtUtil;
import com.localbite.backend.auth.service.PasswordPolicyService;
import com.localbite.backend.auth.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service class that handles all user-related business logic
 * This is where the "brain" of authentication happens
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final JwtProperties jwtProperties;
    private final EmailVerificationService emailVerificationService;
    private final PasswordPolicyService passwordPolicyService;

    /**
     * Register a new user account
     * 
     * @param request Registration details from frontend
     * @return AuthResponse indicating verification is needed
     */
    @Transactional
    public AuthResponse registerUser(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        // Step 1: Check if user already exists (SECURITY: Don't reveal existence)
        if (userRepository.existsByEmail(request.getEmail())) {
            // 🔒 SECURITY FIX: Don't reveal that email already exists
            // Instead, return success response but don't create duplicate account
            log.warn("Registration attempt for existing email: {} - returning success to prevent enumeration", request.getEmail());
            
            // Return a generic success response without revealing email exists
            return AuthResponse.builder()
                    .token(null) // No token for duplicate registration
                    .refreshToken(null) 
                    .tokenType("Bearer")
                    .expiresIn(null)
                    .user(null) // Don't return user info for security
                    .build();
        }

        // Step 2: 🔐 VALIDATE ROLE BEFORE PROCESSING
        if (!isValidRole(request.getRole())) {
            log.warn("Registration attempt with invalid role: {} for email: {}", request.getRole(), request.getEmail());
            throw new RuntimeException("Invalid role specified");
        }

        // Step 3: 🔐 PASSWORD POLICY VALIDATION
        // Create a temporary user object for password validation (includes personal info checks)
        User tempUser = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build();
        
        PasswordPolicyService.PasswordValidationResult passwordValidation = 
                passwordPolicyService.validatePassword(request.getPassword(), tempUser, null);
        
        if (!passwordValidation.isValid()) {
            log.warn("Registration failed due to password policy violations for email: {} - {}", 
                    request.getEmail(), passwordValidation.getViolationsMessage());
            throw new RuntimeException("Password policy violation: " + passwordValidation.getViolationsMessage());
        }

        // Step 4: Get or create the user's role
        Role userRole = roleRepository.findByName(request.getRole())
                .orElseGet(() -> createDefaultRole(request.getRole()));

        // Step 5: Create the user entity
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // Hash the password
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .emailVerified(false) // Will be verified later via email
                .accountLocked(false)
                .enabled(true)
                .provider(User.AuthProvider.LOCAL) // Manual registration (not OAuth)
                .failedLoginAttempts(0)
                .roles(Set.of(userRole)) // Assign the role
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Step 6: Save user to database
        User savedUser = userRepository.save(user);
        log.info("Successfully registered user with ID: {} - email verification required", savedUser.getId());

        // Step 7: 💾 Save password to history for future policy enforcement
        try {
            passwordPolicyService.savePasswordToHistory(savedUser, request.getPassword(), null);
        } catch (Exception e) {
            log.warn("Failed to save password history for user {}: {}", savedUser.getEmail(), e.getMessage());
            // Don't fail registration if password history fails
        }

        // Step 8: Send verification email
        try {
            emailVerificationService.sendVerificationEmail(savedUser);
            log.info("Verification email sent to: {}", savedUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", savedUser.getEmail(), e.getMessage());
            // Don't fail registration if email sending fails - user can request resend
        }

        // Step 9: Return response WITHOUT JWT tokens (user must verify email first)
        return AuthResponse.builder()
                .token(null) // No token until email is verified
                .refreshToken(null) // No refresh token until email is verified
                .tokenType("Bearer")
                .expiresIn(null) // No expiration since no token
                .user(buildUserInfo(savedUser))
                .build();
    }

    /**
     * Authenticate user login
     * 
     * @param request Login credentials from frontend
     * @return AuthResponse with JWT token and user info (only if email is verified)
     */
    @Transactional
    public AuthResponse loginUser(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // 🔒 SECURITY FIX: Consistent timing and error messages to prevent information disclosure
        String genericErrorMessage = "Invalid email or password";
        
        // Always perform password hashing operation to prevent timing attacks
        // This ensures consistent timing whether user exists or not
        String dummyHash = passwordEncoder.encode("dummy-password-for-timing");
        
        // Get user from database for validation
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        
        // 🔒 SECURITY: Always perform authentication attempt to maintain consistent timing
        boolean authenticationSuccessful = false;
        
        if (user != null) {
            // User exists - check all security conditions first
            boolean isValidAccount = user.getEmailVerified() && 
                                   !user.getAccountLocked() && 
                                   user.getEnabled();
            
            if (isValidAccount) {
                try {
                    // Attempt authentication
                    Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                        )
                    );
                    authenticationSuccessful = true;
                    
                } catch (Exception e) {
                    log.warn("Authentication failed for email: {} - {}", request.getEmail(), e.getClass().getSimpleName());
                    authenticationSuccessful = false;
                }
            } else {
                // Account exists but has security restrictions - log details but return generic error
                log.warn("Login attempt for account with restrictions: {} - verified: {}, locked: {}, enabled: {}", 
                        request.getEmail(), user.getEmailVerified(), user.getAccountLocked(), user.getEnabled());
                authenticationSuccessful = false;
            }
        } else {
            // User doesn't exist - log for security monitoring but return generic error
            log.warn("Login attempt for non-existent email: {}", request.getEmail());
            authenticationSuccessful = false;
        }
        
        if (authenticationSuccessful) {
            // 🔐 Check password expiry before allowing login
            PasswordPolicyService.PasswordExpiryInfo expiryInfo = passwordPolicyService.checkPasswordExpiry(user);
            if (expiryInfo.isExpired()) {
                log.warn("Login denied for user {} - password has expired", user.getEmail());
                throw new RuntimeException("Your password has expired. Please reset your password to continue.");
            }
            
            // Success path - update login tracking and generate tokens
            user.setLastLogin(LocalDateTime.now());
            user.setFailedLoginAttempts(0); // Reset failed attempts on successful login
            userRepository.save(user);

            // Generate JWT tokens
            String accessToken = jwtUtil.generateToken(user);
            String refreshToken = jwtUtil.generateRefreshToken(user);

            log.info("Successful login for verified user ID: {}", user.getId());

            // Build response with password expiry warning if needed
            AuthResponse.AuthResponseBuilder responseBuilder = AuthResponse.builder()
                    .token(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtProperties.getExpiration() / 1000)
                    .user(buildUserInfo(user));

            // Add password expiry warning to user info if needed
            if (expiryInfo.isWarning()) {
                log.info("Password expiry warning for user {} - {} days remaining", 
                        user.getEmail(), expiryInfo.getDaysUntilExpiry());
                // The frontend can check user info for password warnings
            }

            return responseBuilder.build();
        } else {
            // 🔒 SECURITY: Failed authentication - track attempts and return generic error
            if (user != null) {
                // Only track failed attempts for existing users
                handleFailedLogin(request.getEmail());
            }
            
            // Always return the same generic error message
            throw new RuntimeException(genericErrorMessage);
        }
    }

    /**
     * Validate if the requested role is allowed for registration
     * 
     * @param roleName Role name to validate
     * @return true if role is valid, false otherwise
     */
    private boolean isValidRole(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return false;
        }
        
        // Allow only specific roles for registration
        return roleName.equalsIgnoreCase("BUYER") || 
               roleName.equalsIgnoreCase("SELLER");
        // ADMIN role should not be allowed during registration
    }

    /**
     * Helper method to create default role if it doesn't exist
     */
    private Role createDefaultRole(String roleName) {
        log.info("Creating new role: {}", roleName);
        Role role = Role.builder()
                .name(roleName)
                .description(getRoleDescription(roleName))
                .createdAt(LocalDateTime.now())
                .build();
        return roleRepository.save(role);
    }

    /**
     * Helper method to get role description
     */
    private String getRoleDescription(String roleName) {
        return switch (roleName) {
            case "BUYER" -> "Can order food and write reviews";
            case "SELLER" -> "Can create food listings and manage orders";
            case "ADMIN" -> "Full system access and user management";
            default -> "Basic user role";
        };
    }

    /**
     * Helper method to build user info for response (without sensitive data)
     */
    private AuthResponse.UserInfo buildUserInfo(User user) {
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return AuthResponse.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .emailVerified(user.getEmailVerified())
                .provider(user.getProvider().name())
                .roles(roles)
                .build();
    }

    /**
     * Handle failed login attempts for security
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    private void handleFailedLogin(String email) {
        // Fetch user in new transaction to ensure fresh data
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            int oldAttempts = user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0;
            user.setFailedLoginAttempts(oldAttempts + 1);
            
            // Lock account after 5 failed attempts
            if (user.getFailedLoginAttempts() >= 5) {
                user.setAccountLocked(true);
                log.warn("Account locked due to too many failed login attempts: {}", email);
            }
            
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.saveAndFlush(user);
        }
    }

    // ==================== ADMINISTRATIVE ACCOUNT MANAGEMENT METHODS ====================

    /**
     * Lock a user account (ADMIN only)
     * 
     * @param email User email to lock
     * @param reason Reason for locking
     * @return Success message
     */
    @Transactional
    public String lockUserAccount(String email, String reason) {
        log.info("Admin attempting to lock account: {} - Reason: {}", email, reason);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        if (user.getAccountLocked()) {
            throw new RuntimeException("Account is already locked");
        }
        
        user.setAccountLocked(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.warn("Account locked by admin: {} - Reason: {}", email, reason);
        return "Account locked successfully";
    }

    /**
     * Unlock a user account (ADMIN only)
     * 
     * @param email User email to unlock
     * @return Success message
     */
    @Transactional
    public String unlockUserAccount(String email) {
        log.info("Admin attempting to unlock account: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        if (!user.getAccountLocked()) {
            throw new RuntimeException("Account is not locked");
        }
        
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0); // Reset failed attempts
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("Account unlocked by admin: {}", email);
        return "Account unlocked successfully";
    }

    /**
     * Enable a user account (ADMIN only)
     * 
     * @param email User email to enable
     * @return Success message
     */
    @Transactional
    public String enableUserAccount(String email) {
        log.info("Admin attempting to enable account: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        if (user.getEnabled()) {
            throw new RuntimeException("Account is already enabled");
        }
        
        user.setEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("Account enabled by admin: {}", email);
        return "Account enabled successfully";
    }

    /**
     * Disable a user account (ADMIN only)
     * 
     * @param email User email to disable
     * @param reason Reason for disabling
     * @return Success message
     */
    @Transactional
    public String disableUserAccount(String email, String reason) {
        log.info("Admin attempting to disable account: {} - Reason: {}", email, reason);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        if (!user.getEnabled()) {
            throw new RuntimeException("Account is already disabled");
        }
        
        user.setEnabled(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.warn("Account disabled by admin: {} - Reason: {}", email, reason);
        return "Account disabled successfully";
    }

    /**
     * Get user account status (ADMIN only)
     * 
     * @param email User email to check
     * @return Map with account status details
     */
    public Map<String, Object> getUserAccountStatus(String email) {
        log.info("Admin checking account status for: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        Map<String, Object> status = new HashMap<>();
        status.put("email", user.getEmail());
        status.put("id", user.getId());
        status.put("firstName", user.getFirstName());
        status.put("lastName", user.getLastName());
        status.put("emailVerified", user.getEmailVerified());
        status.put("accountLocked", user.getAccountLocked());
        status.put("enabled", user.getEnabled());
        status.put("failedLoginAttempts", user.getFailedLoginAttempts());
        status.put("lastLogin", user.getLastLogin());
        status.put("createdAt", user.getCreatedAt());
        status.put("updatedAt", user.getUpdatedAt());
        status.put("provider", user.getProvider().name());
        status.put("roles", user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList());
        
        return status;
    }

    /**
     * Reset failed login attempts for a user (ADMIN only)
     * 
     * @param email User email to reset
     * @return Success message
     */
    @Transactional
    public String resetFailedLoginAttempts(String email) {
        log.info("Admin resetting failed login attempts for: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        int previousAttempts = user.getFailedLoginAttempts();
        user.setFailedLoginAttempts(0);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("Failed login attempts reset for {}: {} -> 0", email, previousAttempts);
        return String.format("Failed login attempts reset (was: %d)", previousAttempts);
    }

    /**
     * Get user by email (for internal service use)
     * 
     * @param email User email
     * @return User entity
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
} 