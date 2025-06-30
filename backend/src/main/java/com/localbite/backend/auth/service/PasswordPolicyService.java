package com.localbite.backend.auth.service;

import com.localbite.backend.auth.config.PasswordPolicyProperties;
import com.localbite.backend.auth.entity.PasswordHistory;
import com.localbite.backend.auth.entity.User;
import com.localbite.backend.auth.repository.PasswordHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordPolicyService {

    private final PasswordPolicyProperties passwordPolicy;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    // Common weak passwords list (top 100 most common passwords)
    private static final Set<String> COMMON_PASSWORDS = Set.of(
        "password", "123456", "password123", "admin", "qwerty", "letmein", "welcome",
        "monkey", "1234567890", "abc123", "111111", "dragon", "master", "sunshine",
        "iloveyou", "princess", "football", "123123", "lovely", "secret", "password1",
        "12345678", "123456789", "qwerty123", "welcome123", "admin123",
        "password12", "123abc", "welcome1", "hello123", "user", "guest", "test",
        "1234", "12345", "654321", "superman", "batman", "computer", "internet"
    );

    /**
     * Validate password against all policy rules
     */
    public PasswordValidationResult validatePassword(String password, User user, HttpServletRequest request) {
        if (!passwordPolicy.isEnforcementEnabled()) {
            return PasswordValidationResult.success();
        }

        List<String> violations = new ArrayList<>();
        PasswordPolicyProperties adjustedPolicy = passwordPolicy.getAdjustedForEnforcementLevel();

        // Basic length validation
        validateLength(password, adjustedPolicy, violations);

        // Character composition validation
        validateCharacterComposition(password, adjustedPolicy, violations);

        // Advanced security checks
        if (adjustedPolicy.isNoCommonPasswords()) {
            validateCommonPasswords(password, violations);
        }

        if (adjustedPolicy.isNoPersonalInfo() && user != null) {
            validatePersonalInfo(password, user, violations);
        }

        if (adjustedPolicy.isNoKeyboardPatterns()) {
            validateKeyboardPatterns(password, adjustedPolicy, violations);
        }

        // Password reuse validation
        if (user != null && adjustedPolicy.getRememberPreviousPasswords() > 0) {
            validatePasswordHistory(password, user, adjustedPolicy, violations);
        }

        // Character repetition validation
        validateRepeatedCharacters(password, adjustedPolicy, violations);

        boolean isValid = violations.isEmpty();
        
        if (isValid) {
            log.info("✅ Password validation successful for user: {}", 
                    user != null ? user.getEmail() : "unknown");
        } else {
            log.warn("🚫 Password validation failed for user: {} - {} violations", 
                    user != null ? user.getEmail() : "unknown", violations.size());
        }

        return new PasswordValidationResult(isValid, violations, generatePasswordRequirements(adjustedPolicy));
    }

    /**
     * Save password to history for future validation
     */
    @Transactional
    public void savePasswordToHistory(User user, String password, HttpServletRequest request) {
        if (!passwordPolicy.isEnforcementEnabled() || passwordPolicy.getRememberPreviousPasswords() <= 0) {
            return;
        }

        try {
            String passwordHash = passwordEncoder.encode(password);
            
            PasswordHistory passwordHistory = PasswordHistory.builder()
                    .user(user)
                    .passwordHash(passwordHash)
                    .ipAddress(getClientIpAddress(request))
                    .userAgent(request != null ? request.getHeader("User-Agent") : null)
                    .build();

            passwordHistoryRepository.save(passwordHistory);

            // Clean up old password history entries
            cleanupOldPasswordHistory(user);

            log.info("💾 Password saved to history for user: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to save password history for user {}: {}", user.getEmail(), e.getMessage());
        }
    }

    /**
     * Check if password has expired and user needs to change it
     */
    public PasswordExpiryInfo checkPasswordExpiry(User user) {
        if (!passwordPolicy.isEnforcementEnabled() || passwordPolicy.getExpiryDays() <= 0) {
            return new PasswordExpiryInfo(false, false, 0);
        }

        LocalDateTime passwordCreatedAt = user.getUpdatedAt() != null ? user.getUpdatedAt() : user.getCreatedAt();
        LocalDateTime expiryDate = passwordCreatedAt.plusDays(passwordPolicy.getExpiryDays());
        LocalDateTime warningDate = expiryDate.minusDays(passwordPolicy.getWarnBeforeExpiryDays());

        boolean isExpired = LocalDateTime.now().isAfter(expiryDate);
        boolean isWarning = LocalDateTime.now().isAfter(warningDate) && !isExpired;
        long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), expiryDate);

        return new PasswordExpiryInfo(isExpired, isWarning, Math.max(0, daysUntilExpiry));
    }

    /**
     * Generate strong password suggestions
     */
    public List<String> generatePasswordSuggestions() {
        List<String> suggestions = new ArrayList<>();
        PasswordPolicyProperties policy = passwordPolicy.getAdjustedForEnforcementLevel();

        suggestions.add("Use a mix of uppercase and lowercase letters");
        if (policy.isRequireDigits()) {
            suggestions.add("Include at least " + policy.getMinDigits() + " number(s)");
        }
        if (policy.isRequireSpecialChars()) {
            suggestions.add("Include at least " + policy.getMinSpecialChars() + " special character(s): " + 
                          policy.getAllowedSpecialChars());
        }
        suggestions.add("Make it at least " + policy.getMinLength() + " characters long");
        if (policy.isNoCommonPasswords()) {
            suggestions.add("Avoid common passwords like 'password123' or 'qwerty'");
        }
        if (policy.isNoPersonalInfo()) {
            suggestions.add("Don't use personal information like your name or email");
        }
        suggestions.add("Consider using a passphrase with multiple words");

        return suggestions;
    }

    // Private validation methods

    private void validateLength(String password, PasswordPolicyProperties policy, List<String> violations) {
        if (password.length() < policy.getMinLength()) {
            violations.add("Password must be at least " + policy.getMinLength() + " characters long");
        }
        if (password.length() > policy.getMaxLength()) {
            violations.add("Password must not exceed " + policy.getMaxLength() + " characters");
        }
    }

    private void validateCharacterComposition(String password, PasswordPolicyProperties policy, List<String> violations) {
        if (policy.isRequireUppercase() && !password.matches(".*[A-Z].*")) {
            violations.add("Password must contain at least one uppercase letter");
        }
        if (policy.isRequireLowercase() && !password.matches(".*[a-z].*")) {
            violations.add("Password must contain at least one lowercase letter");
        }
        if (policy.isRequireDigits()) {
            long digitCount = password.chars().filter(Character::isDigit).count();
            if (digitCount < policy.getMinDigits()) {
                violations.add("Password must contain at least " + policy.getMinDigits() + " digit(s)");
            }
        }
        if (policy.isRequireSpecialChars()) {
            String specialChars = policy.getAllowedSpecialChars();
            long specialCharCount = password.chars()
                    .filter(c -> specialChars.indexOf(c) >= 0)
                    .count();
            if (specialCharCount < policy.getMinSpecialChars()) {
                violations.add("Password must contain at least " + policy.getMinSpecialChars() + 
                             " special character(s) from: " + specialChars);
            }
        }
    }

    private void validateCommonPasswords(String password, List<String> violations) {
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            violations.add("Password is too common and easily guessable");
        }
    }

    private void validatePersonalInfo(String password, User user, List<String> violations) {
        String passwordLower = password.toLowerCase();
        
        if (user.getFirstName() != null && passwordLower.contains(user.getFirstName().toLowerCase())) {
            violations.add("Password should not contain your first name");
        }
        if (user.getLastName() != null && passwordLower.contains(user.getLastName().toLowerCase())) {
            violations.add("Password should not contain your last name");
        }
        if (user.getEmail() != null) {
            String emailUsername = user.getEmail().split("@")[0].toLowerCase();
            if (passwordLower.contains(emailUsername)) {
                violations.add("Password should not contain your email username");
            }
        }
    }

    private void validateKeyboardPatterns(String password, PasswordPolicyProperties policy, List<String> violations) {
        String passwordLower = password.toLowerCase();
        for (String pattern : policy.getKeyboardPatterns()) {
            if (passwordLower.contains(pattern)) {
                violations.add("Password should not contain keyboard patterns like '" + pattern + "'");
                break;
            }
        }
    }

    private void validatePasswordHistory(String password, User user, PasswordPolicyProperties policy, List<String> violations) {
        try {
            // Skip password history check for new users (not yet saved to database)
            if (user.getId() == null) {
                log.debug("Skipping password history check for new user: {}", user.getEmail());
                return;
            }
            
            List<PasswordHistory> recentPasswords = passwordHistoryRepository.findRecentPasswordHistory(user);
            int checkCount = Math.min(policy.getRememberPreviousPasswords(), recentPasswords.size());
            
            for (int i = 0; i < checkCount; i++) {
                PasswordHistory history = recentPasswords.get(i);
                if (passwordEncoder.matches(password, history.getPasswordHash())) {
                    violations.add("Password has been used recently. Please choose a different password.");
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check password history for user {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private void validateRepeatedCharacters(String password, PasswordPolicyProperties policy, List<String> violations) {
        if (policy.getMaxRepeatedChars() > 0) {
            Pattern repeatedPattern = Pattern.compile("(.)\\1{" + policy.getMaxRepeatedChars() + ",}");
            if (repeatedPattern.matcher(password).find()) {
                violations.add("Password should not contain more than " + policy.getMaxRepeatedChars() + 
                             " consecutive identical characters");
            }
        }
    }

    private void cleanupOldPasswordHistory(User user) {
        try {
            List<PasswordHistory> allHistory = passwordHistoryRepository.findByUserOrderByCreatedAtDesc(user);
            if (allHistory.size() > passwordPolicy.getRememberPreviousPasswords()) {
                List<PasswordHistory> toDelete = allHistory.subList(passwordPolicy.getRememberPreviousPasswords(), allHistory.size());
                passwordHistoryRepository.deleteAll(toDelete);
                log.debug("Cleaned up {} old password history entries for user: {}", toDelete.size(), user.getEmail());
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup password history for user {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private String generatePasswordRequirements(PasswordPolicyProperties policy) {
        StringBuilder requirements = new StringBuilder();
        requirements.append("Password requirements: ");
        requirements.append(policy.getMinLength()).append("-").append(policy.getMaxLength()).append(" characters");
        
        if (policy.isRequireUppercase() || policy.isRequireLowercase() || 
            policy.isRequireDigits() || policy.isRequireSpecialChars()) {
            requirements.append(", must include ");
            List<String> reqList = new ArrayList<>();
            if (policy.isRequireUppercase()) reqList.add("uppercase letters");
            if (policy.isRequireLowercase()) reqList.add("lowercase letters");
            if (policy.isRequireDigits()) reqList.add("numbers");
            if (policy.isRequireSpecialChars()) reqList.add("special characters");
            requirements.append(String.join(", ", reqList));
        }
        
        return requirements.toString();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return "unknown";
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // Result classes
    public static class PasswordValidationResult {
        private final boolean valid;
        private final List<String> violations;
        private final String requirements;

        public PasswordValidationResult(boolean valid, List<String> violations, String requirements) {
            this.valid = valid;
            this.violations = violations != null ? violations : new ArrayList<>();
            this.requirements = requirements;
        }

        public static PasswordValidationResult success() {
            return new PasswordValidationResult(true, new ArrayList<>(), "");
        }

        public boolean isValid() { return valid; }
        public List<String> getViolations() { return violations; }
        public String getRequirements() { return requirements; }
        public String getViolationsMessage() {
            return violations.isEmpty() ? "" : String.join("; ", violations);
        }
    }

    public static class PasswordExpiryInfo {
        private final boolean expired;
        private final boolean warning;
        private final long daysUntilExpiry;

        public PasswordExpiryInfo(boolean expired, boolean warning, long daysUntilExpiry) {
            this.expired = expired;
            this.warning = warning;
            this.daysUntilExpiry = daysUntilExpiry;
        }

        public boolean isExpired() { return expired; }
        public boolean isWarning() { return warning; }
        public long getDaysUntilExpiry() { return daysUntilExpiry; }
    }
} 