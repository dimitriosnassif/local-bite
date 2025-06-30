package com.localbite.backend.auth.security;

import com.localbite.backend.auth.config.JwtProperties;
import com.localbite.backend.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {

    private final JwtProperties jwtProperties;
    
    // Cache the signing key for better performance and thread safety
    private volatile SecretKey cachedSigningKey;

    @PostConstruct
    public void initializeSigningKey() {
        try {
            // Pre-compute and validate the signing key at startup
            this.cachedSigningKey = createSigningKey();
            log.info("✅ JWT signing key initialized successfully");
        } catch (Exception e) {
            log.error("🚨 CRITICAL: Failed to initialize JWT signing key: {}", e.getMessage());
            throw new IllegalStateException("JWT signing key initialization failed", e);
        }
    }

    private SecretKey createSigningKey() {
        try {
            String secret = jwtProperties.getSecret();
            if (secret == null || secret.trim().isEmpty()) {
                throw new IllegalStateException("JWT secret is null or empty");
            }

            // Attempt to decode the Base64 secret with proper error handling
            byte[] keyBytes = Decoders.BASE64.decode(secret.trim());
            
            // Validate key length (minimum 256 bits for HS512)
            if (keyBytes.length < 32) {
                throw new IllegalStateException(
                    String.format("JWT secret key is too short: %d bytes (minimum 32 bytes required)", 
                                keyBytes.length));
            }

            return Keys.hmacShaKeyFor(keyBytes);
            
        } catch (IllegalArgumentException e) {
            log.error("🚨 JWT secret is not valid Base64: {}", e.getMessage());
            throw new IllegalStateException(
                "JWT secret must be a valid Base64-encoded string. " +
                "Generate one with: openssl rand -base64 64", e);
        } catch (Exception e) {
            log.error("🚨 Unexpected error creating JWT signing key: {}", e.getMessage());
            throw new IllegalStateException("Failed to create JWT signing key", e);
        }
    }

    private SecretKey getSigningKey() {
        // Return the cached key - no need to decode on every operation
        if (cachedSigningKey == null) {
            // Fallback initialization (should not happen due to @PostConstruct)
            synchronized (this) {
                if (cachedSigningKey == null) {
                    cachedSigningKey = createSigningKey();
                }
            }
        }
        return cachedSigningKey;
    }

    public String generateToken(User user) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("firstName", user.getFirstName());
            claims.put("lastName", user.getLastName());
            claims.put("emailVerified", user.getEmailVerified());
            claims.put("roles", user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));
            claims.put("provider", user.getProvider().name());

            return createToken(claims, user.getEmail(), jwtProperties.getExpiration());
        } catch (Exception e) {
            log.error("Failed to generate JWT token for user {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("JWT token generation failed", e);
        }
    }

    public String generateRefreshToken(User user) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("tokenType", "refresh");

            return createToken(claims, user.getEmail(), jwtProperties.getRefreshExpiration());
        } catch (Exception e) {
            log.error("Failed to generate refresh token for user {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Refresh token generation failed", e);
        }
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        try {
            return Jwts.builder()
                    .claims(claims)
                    .subject(subject)
                    .issuer(jwtProperties.getIssuer())
                    .audience().add(jwtProperties.getAudience()).and()
                    .issuedAt(new Date(System.currentTimeMillis()))
                    .expiration(new Date(System.currentTimeMillis() + expiration))
                    .signWith(getSigningKey())
                    .compact();
        } catch (Exception e) {
            log.error("Failed to create JWT token: {}", e.getMessage());
            throw new RuntimeException("JWT token creation failed", e);
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("JWT token is expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.warn("JWT token is malformed: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            log.warn("JWT signature is invalid: {}", e.getMessage());
            throw e;
        } catch (SecurityException e) {
            log.warn("JWT security validation failed: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("JWT token or handler arguments are invalid: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during JWT token parsing: {}", e.getMessage());
            throw new JwtException("JWT token parsing failed", e);
        }
    }

    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            log.warn("Error checking token expiration: {}", e.getMessage());
            return true; // Treat any error as expired for security
        }
    }

    public Boolean validateToken(String token, User user) {
        try {
            if (token == null || token.trim().isEmpty()) {
                log.debug("Token validation failed: token is null or empty");
                return false;
            }
            
            if (user == null || user.getEmail() == null) {
                log.debug("Token validation failed: user or email is null");
                return false;
            }
            
            final String username = extractUsername(token);
            boolean isValid = username.equals(user.getEmail()) && !isTokenExpired(token);
            
            if (!isValid) {
                log.debug("Token validation failed for user: {}", user.getEmail());
            }
            
            return isValid;
        } catch (JwtException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Unexpected error during token validation: {}", e.getMessage());
            return false;
        }
    }

    public Boolean isRefreshToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }
            
            Claims claims = extractAllClaims(token);
            return "refresh".equals(claims.get("tokenType"));
        } catch (JwtException e) {
            log.debug("Error checking if token is refresh token: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Unexpected error checking refresh token: {}", e.getMessage());
            return false;
        }
    }
} 