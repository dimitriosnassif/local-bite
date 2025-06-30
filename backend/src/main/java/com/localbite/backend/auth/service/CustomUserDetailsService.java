package com.localbite.backend.auth.service;

import com.localbite.backend.auth.entity.User;
import com.localbite.backend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom UserDetailsService for Spring Security.
 * This is how Spring Security loads user information during authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load user by username (email in our case) for Spring Security authentication
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user details for email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    // 🔒 SECURITY FIX: Don't include email in exception message to prevent information disclosure
                    log.warn("User lookup failed for authentication attempt");
                    return new UsernameNotFoundException("Authentication failed");
                });
        
        log.debug("Successfully loaded user: {} with roles: {}", 
            user.getEmail(), user.getAuthorities());
        
        return user; // User implements UserDetails, so we can return it directly
    }
} 