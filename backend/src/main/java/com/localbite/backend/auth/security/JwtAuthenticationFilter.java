package com.localbite.backend.auth.security;

import com.localbite.backend.auth.entity.User;
import com.localbite.backend.auth.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtUtil.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Optional<User> userOptional = userRepository.findByEmail(userEmail);
                
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    
                    if (jwtUtil.validateToken(jwt, user) && isAccountValid(user)) {
                        UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(
                                user, 
                                null, 
                                user.getAuthorities()
                            );
                        
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.debug("User '{}' authenticated successfully", userEmail);
                    } else {
                        log.debug("Authentication failed for user '{}' - invalid token or account status", userEmail);
                    }
                } else {
                    log.debug("User not found for email: {}", userEmail);
                }
            }
        } catch (JwtException e) {
            log.debug("JWT authentication failed: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Comprehensive account validation to ensure the user is allowed to authenticate
     */
    private boolean isAccountValid(User user) {
        if (user == null) {
            log.debug("Account validation failed: user is null");
            return false;
        }

        // Check if account is enabled
        if (!user.getEnabled()) {
            log.debug("Account validation failed for user {}: account is disabled", user.getEmail());
            return false;
        }

        // Check if account is locked
        if (user.getAccountLocked()) {
            log.debug("Account validation failed for user {}: account is locked", user.getEmail());
            return false;
        }

        // 🚨 CRITICAL: Check if email is verified
        if (!user.getEmailVerified()) {
            log.debug("Account validation failed for user {}: email not verified", user.getEmail());
            return false;
        }

        // All checks passed
        return true;
    }
} 