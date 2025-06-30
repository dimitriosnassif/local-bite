package com.localbite.backend.auth.service;

import com.localbite.backend.auth.entity.Role;
import com.localbite.backend.auth.entity.User;
import com.localbite.backend.auth.repository.RoleRepository;
import com.localbite.backend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        try {
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user: {}", ex.getMessage(), ex);
            throw new OAuth2AuthenticationException("Error processing OAuth2 user: " + ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oauth2User.getAttributes());
        
        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        User user = userRepository.findByEmail(userInfo.getEmail()).orElse(null);
        
        if (user != null) {
            user = updateExistingUser(user, userInfo, registrationId);
        } else {
            user = registerNewUser(userInfo, registrationId);
        }

        return CustomOAuth2User.create(user, oauth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserInfo userInfo, String registrationId) {
        log.info("Registering new OAuth2 user with email: {} from provider: {}", userInfo.getEmail(), registrationId);
        
        User.AuthProvider provider = User.AuthProvider.valueOf(registrationId.toUpperCase());
        Role defaultRole = roleRepository.findByName("BUYER")
                .orElseGet(() -> createDefaultRole("BUYER"));

        User user = User.builder()
                .email(userInfo.getEmail())
                .firstName(userInfo.getFirstName())
                .lastName(userInfo.getLastName())
                .emailVerified(true) // OAuth providers handle email verification
                .provider(provider)
                .providerId(userInfo.getId())
                .accountLocked(false)
                .enabled(true)
                .failedLoginAttempts(0)
                .roles(Set.of(defaultRole))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("Successfully registered OAuth2 user with ID: {} from provider: {}", savedUser.getId(), registrationId);
        
        return savedUser;
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo userInfo, String registrationId) {
        log.info("Updating existing user: {} with OAuth2 info from provider: {}", existingUser.getEmail(), registrationId);
        
        User.AuthProvider provider = User.AuthProvider.valueOf(registrationId.toUpperCase());
        
        // Update user info if needed
        boolean needsUpdate = false;
        
        if (!provider.equals(existingUser.getProvider())) {
            existingUser.setProvider(provider);
            existingUser.setProviderId(userInfo.getId());
            needsUpdate = true;
        }
        
        if (!Boolean.TRUE.equals(existingUser.getEmailVerified())) {
            existingUser.setEmailVerified(true);
            needsUpdate = true;
        }
        
        if (userInfo.getFirstName() != null && !userInfo.getFirstName().equals(existingUser.getFirstName())) {
            existingUser.setFirstName(userInfo.getFirstName());
            needsUpdate = true;
        }
        
        if (userInfo.getLastName() != null && !userInfo.getLastName().equals(existingUser.getLastName())) {
            existingUser.setLastName(userInfo.getLastName());
            needsUpdate = true;
        }
        
        if (needsUpdate) {
            existingUser.setUpdatedAt(LocalDateTime.now());
            existingUser = userRepository.save(existingUser);
            log.info("Updated existing user: {} with OAuth2 provider: {}", existingUser.getEmail(), registrationId);
        }
        
        return existingUser;
    }

    private Role createDefaultRole(String roleName) {
        log.info("Creating default role: {}", roleName);
        Role role = Role.builder()
                .name(roleName)
                .description("Default " + roleName.toLowerCase() + " role")
                .createdAt(LocalDateTime.now())
                .build();
        return roleRepository.save(role);
    }

    // OAuth2 User Info abstraction
    public static abstract class OAuth2UserInfo {
        protected Map<String, Object> attributes;

        public OAuth2UserInfo(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        public abstract String getId();
        public abstract String getFirstName();
        public abstract String getLastName();
        public abstract String getEmail();
        public abstract String getImageUrl();
    }

    // Google OAuth2 User Info
    public static class GoogleOAuth2UserInfo extends OAuth2UserInfo {
        public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
            super(attributes);
        }

        @Override
        public String getId() {
            return (String) attributes.get("sub");
        }

        @Override
        public String getFirstName() {
            return (String) attributes.get("given_name");
        }

        @Override
        public String getLastName() {
            return (String) attributes.get("family_name");
        }

        @Override
        public String getEmail() {
            return (String) attributes.get("email");
        }

        @Override
        public String getImageUrl() {
            return (String) attributes.get("picture");
        }
    }

    // Facebook OAuth2 User Info
    public static class FacebookOAuth2UserInfo extends OAuth2UserInfo {
        public FacebookOAuth2UserInfo(Map<String, Object> attributes) {
            super(attributes);
        }

        @Override
        public String getId() {
            return (String) attributes.get("id");
        }

        @Override
        public String getFirstName() {
            return (String) attributes.get("first_name");
        }

        @Override
        public String getLastName() {
            return (String) attributes.get("last_name");
        }

        @Override
        public String getEmail() {
            return (String) attributes.get("email");
        }

        @Override
        public String getImageUrl() {
            return "https://graph.facebook.com/" + getId() + "/picture?type=large";
        }
    }

    // Factory to create OAuth2UserInfo instances
    public static class OAuth2UserInfoFactory {
        public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
            if ("google".equalsIgnoreCase(registrationId)) {
                return new GoogleOAuth2UserInfo(attributes);
            } else if ("facebook".equalsIgnoreCase(registrationId)) {
                return new FacebookOAuth2UserInfo(attributes);
            } else {
                throw new OAuth2AuthenticationException("Sorry! Login with " + registrationId + " is not supported yet.");
            }
        }
    }
} 