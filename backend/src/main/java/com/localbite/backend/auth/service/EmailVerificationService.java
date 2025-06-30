package com.localbite.backend.auth.service;

import com.localbite.backend.auth.entity.User;
import com.localbite.backend.auth.entity.VerificationToken;
import com.localbite.backend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for handling email verification functionality
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final UserRepository userRepository;
    // TODO: Add VerificationTokenRepository when implemented
    // private final VerificationTokenRepository verificationTokenRepository;
    // TODO: Add EmailService when implemented  
    // private final EmailService emailService;

    /**
     * Send verification email to user
     * 
     * @param user User to send verification email to
     */
    public void sendVerificationEmail(User user) {
        if (user.getEmailVerified()) {
            log.info("User {} is already verified, skipping verification email", user.getEmail());
            return;
        }

        try {
            // Generate verification token
            String token = generateVerificationToken();
            
            // TODO: Save token to database
            // VerificationToken verificationToken = VerificationToken.builder()
            //     .token(token)
            //     .user(user)
            //     .expiryDate(LocalDateTime.now().plusHours(24)) // 24 hour expiry
            //     .used(false)
            //     .build();
            // verificationTokenRepository.save(verificationToken);

            // TODO: Send email
            // emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), token);
            
            log.info("✅ Verification email would be sent to: {} with token: {}", user.getEmail(), token);
            log.info("📧 TODO: Implement actual email sending service");
            
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    /**
     * Verify user email using verification token
     * 
     * @param token Verification token
     * @return true if verification successful, false otherwise
     */
    @Transactional
    public boolean verifyEmail(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Verification attempt with empty token");
            return false;
        }

        try {
            // TODO: Implement proper token validation when database repository is ready
            // For now, simulate verification for testing purposes
            log.info("🔧 SIMULATION: Verifying token: {}", token);
            
            // TODO: Replace with actual implementation:
            // Optional<VerificationToken> verificationTokenOpt = verificationTokenRepository.findByToken(token);
            // 
            // if (verificationTokenOpt.isEmpty()) {
            //     log.warn("Invalid verification token: {}", token);
            //     return false;
            // }
            //
            // VerificationToken verificationToken = verificationTokenOpt.get();
            //
            // if (verificationToken.isExpired()) {
            //     log.warn("Expired verification token: {}", token);
            //     return false;
            // }
            //
            // if (verificationToken.getUsed()) {
            //     log.warn("Already used verification token: {}", token);
            //     return false;
            // }
            //
            // User user = verificationToken.getUser();
            // user.setEmailVerified(true);
            // userRepository.save(user);
            //
            // verificationToken.setUsed(true);
            // verificationTokenRepository.save(verificationToken);
            //
            // log.info("✅ Email verified successfully for user: {}", user.getEmail());
            // return true;

            // TEMPORARY: For testing - just log the verification attempt
            log.info("📧 SIMULATION: Email verification successful for token: {}", token);
            return true;

        } catch (Exception e) {
            log.error("Error during email verification: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Resend verification email to user
     * 
     * @param email User email
     */
    public void resendVerificationEmail(String email) {
        try {
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            if (user.getEmailVerified()) {
                throw new RuntimeException("Email is already verified");
            }

            // TODO: Invalidate old tokens
            // verificationTokenRepository.invalidateTokensForUser(user.getId());

            sendVerificationEmail(user);
            log.info("Verification email resent to: {}", email);

        } catch (Exception e) {
            log.error("Failed to resend verification email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to resend verification email", e);
        }
    }

    /**
     * Manually verify a user's email (for admin/testing purposes)
     * 
     * @param email User email to verify
     */
    @Transactional
    public void manuallyVerifyEmail(String email) {
        try {
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            if (user.getEmailVerified()) {
                log.info("User {} is already verified", email);
                return;
            }

            user.setEmailVerified(true);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("✅ Manually verified email for user: {}", email);

        } catch (Exception e) {
            log.error("Failed to manually verify email {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to verify email", e);
        }
    }

    /**
     * Check if user's email is verified
     * 
     * @param email User email
     * @return true if verified, false otherwise
     */
    public boolean isEmailVerified(String email) {
        return userRepository.findByEmail(email)
            .map(User::getEmailVerified)
            .orElse(false);
    }

    /**
     * Generate a secure verification token
     * 
     * @return Generated token
     */
    private String generateVerificationToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
} 