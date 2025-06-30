package com.localbite.backend.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Component
@ConfigurationProperties(prefix = "password.policy")
@Data
@Slf4j
public class PasswordPolicyProperties {

    // Basic strength requirements
    private int minLength = 8;
    private int maxLength = 128;
    private boolean requireUppercase = true;
    private boolean requireLowercase = true;
    private boolean requireDigits = true;
    private boolean requireSpecialChars = true;
    private int minSpecialChars = 1;
    private int minDigits = 1;

    // Advanced policy settings
    private boolean noCommonPasswords = true;
    private boolean noPersonalInfo = true;
    private boolean noKeyboardPatterns = true;
    private int maxRepeatedChars = 3;

    // Password history and expiration
    private int rememberPreviousPasswords = 5;
    private int expiryDays = 90;
    private int warnBeforeExpiryDays = 7;

    // Security enforcement
    private int maxViolationsBeforeLockout = 5;
    private int lockoutDurationMinutes = 30;
    private EnforcementLevel enforcementLevel = EnforcementLevel.STRICT;

    public enum EnforcementLevel {
        DISABLED,   // No password policy enforcement
        LENIENT,    // Basic requirements only
        MODERATE,   // Standard security requirements
        STRICT      // Maximum security requirements
    }

    @PostConstruct
    public void validateConfiguration() {
        log.info("🔐 Password Policy Configuration:");
        log.info("  Enforcement Level: {}", enforcementLevel);
        log.info("  Length Requirements: {}-{} characters", minLength, maxLength);
        log.info("  Character Requirements: Upper={}, Lower={}, Digits={}, Special={}", 
                requireUppercase, requireLowercase, requireDigits, requireSpecialChars);
        log.info("  Advanced Checks: CommonPasswords={}, PersonalInfo={}, KeyboardPatterns={}", 
                noCommonPasswords, noPersonalInfo, noKeyboardPatterns);
        log.info("  Password History: Remember {} previous passwords", rememberPreviousPasswords);
        log.info("  Password Expiry: {} days (warn {} days before)", expiryDays, warnBeforeExpiryDays);
        log.info("  Security: {} violations before lockout for {} minutes", 
                maxViolationsBeforeLockout, lockoutDurationMinutes);

        // Validation
        if (minLength < 4) {
            log.warn("⚠️ Minimum password length is very weak: {} characters", minLength);
        }
        if (maxLength > 256) {
            log.warn("⚠️ Maximum password length is very high: {} characters", maxLength);
        }
        if (minLength > maxLength) {
            throw new IllegalStateException("Minimum password length cannot be greater than maximum length");
        }

        log.info("✅ Password policy configuration validated successfully");
    }

    /**
     * Check if policy should be enforced based on enforcement level
     */
    public boolean isEnforcementEnabled() {
        return enforcementLevel != EnforcementLevel.DISABLED;
    }

    /**
     * Check if strict enforcement is enabled
     */
    public boolean isStrictEnforcement() {
        return enforcementLevel == EnforcementLevel.STRICT;
    }

    /**
     * Get special characters that are allowed/required
     */
    public String getAllowedSpecialChars() {
        return "!@#$%^&*()_+-=[]{}|;:,.<>?";
    }

    /**
     * Get keyboard patterns to detect
     */
    public String[] getKeyboardPatterns() {
        return new String[]{
            "qwerty", "asdf", "zxcv", "1234", "abcd",
            "qwertyuiop", "asdfghjkl", "zxcvbnm",
            "123456789", "987654321", "abcdefgh"
        };
    }

    /**
     * Adjust requirements based on enforcement level
     */
    public PasswordPolicyProperties getAdjustedForEnforcementLevel() {
        PasswordPolicyProperties adjusted = new PasswordPolicyProperties();
        
        switch (enforcementLevel) {
            case DISABLED:
                adjusted.minLength = 1;
                adjusted.requireUppercase = false;
                adjusted.requireLowercase = false;
                adjusted.requireDigits = false;
                adjusted.requireSpecialChars = false;
                adjusted.noCommonPasswords = false;
                adjusted.noPersonalInfo = false;
                adjusted.noKeyboardPatterns = false;
                break;
                
            case LENIENT:
                adjusted = this; // Use basic settings
                adjusted.noCommonPasswords = false;
                adjusted.noKeyboardPatterns = false;
                adjusted.minLength = Math.max(6, this.minLength);
                break;
                
            case MODERATE:
                adjusted = this; // Use most settings
                adjusted.minLength = Math.max(8, this.minLength);
                break;
                
            case STRICT:
            default:
                adjusted = this; // Use all settings
                adjusted.minLength = Math.max(10, this.minLength);
                break;
        }
        
        adjusted.enforcementLevel = this.enforcementLevel;
        return adjusted;
    }
} 