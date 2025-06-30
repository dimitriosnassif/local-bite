package com.localbite.backend.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import java.util.Base64;

@Component
@ConfigurationProperties(prefix = "jwt")
@Data
@Slf4j
public class JwtProperties {

    private String secret;
    private long expiration = 3600000; // 1 hour in milliseconds (reduced from 24h)
    private long refreshExpiration = 604800000; // 7 days in milliseconds
    private String issuer = "LocalBite";
    private String audience = "LocalBite-Users";
    
    @PostConstruct
    public void validateConfiguration() {
        if (secret == null || secret.trim().isEmpty()) {
            log.error("🚨 CRITICAL SECURITY ERROR: JWT secret is not configured!");
            log.error("Set JWT_SECRET environment variable with a secure Base64-encoded secret");
            log.error("Generate one with: openssl rand -base64 64");
            throw new IllegalStateException("JWT secret must be configured via JWT_SECRET environment variable");
        }
        
        // Validate secret is Base64 encoded and sufficient length
        try {
            byte[] decoded = Base64.getDecoder().decode(secret);
            if (decoded.length < 32) { // 256 bits minimum
                log.error("🚨 SECURITY WARNING: JWT secret is too short (< 256 bits)");
                log.error("Generate a longer secret with: openssl rand -base64 64");
                throw new IllegalStateException("JWT secret must be at least 256 bits (32 bytes) long");
            }
            log.info("✅ JWT configuration validated successfully");
        } catch (IllegalArgumentException e) {
            log.error("🚨 SECURITY ERROR: JWT secret is not valid Base64");
            log.error("Generate a proper secret with: openssl rand -base64 64");
            throw new IllegalStateException("JWT secret must be Base64 encoded", e);
        }
    }
} 