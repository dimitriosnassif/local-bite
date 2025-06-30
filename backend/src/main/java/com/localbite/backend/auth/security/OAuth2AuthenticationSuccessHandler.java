package com.localbite.backend.auth.security;

import com.localbite.backend.auth.config.JwtProperties;
import com.localbite.backend.auth.service.CustomOAuth2User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException {
        
        String targetUrl = determineTargetUrl(request, response, authentication);
        
        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) {
        
        try {
            CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
            
            // Generate JWT tokens
            String accessToken = jwtUtil.generateToken(oauth2User.getUser());
            String refreshToken = jwtUtil.generateRefreshToken(oauth2User.getUser());
            
            log.info("OAuth2 login successful for user ID: {} from provider: {}", 
                    oauth2User.getId(), oauth2User.getProvider());

            // Redirect to frontend with tokens as URL parameters
            // In production, consider using HTTP-only cookies instead
            String frontendUrl = "http://localhost:3000/auth/oauth2/redirect";
            
            return UriComponentsBuilder.fromUriString(frontendUrl)
                    .queryParam("token", URLEncoder.encode(accessToken, StandardCharsets.UTF_8))
                    .queryParam("refreshToken", URLEncoder.encode(refreshToken, StandardCharsets.UTF_8))
                    .queryParam("tokenType", "Bearer")
                    .queryParam("expiresIn", jwtProperties.getExpiration() / 1000)
                    .build().toUriString();
                    
        } catch (Exception ex) {
            log.error("Error generating tokens for OAuth2 user: {}", ex.getMessage(), ex);
            
            // Redirect to frontend with error
            return UriComponentsBuilder.fromUriString("http://localhost:3000/auth")
                    .queryParam("error", "oauth2_token_generation_failed")
                    .build().toUriString();
        }
    }
} 