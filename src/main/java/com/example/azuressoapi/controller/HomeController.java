package com.example.azuressoapi.controller;

import com.example.azuressoapi.service.OAuth2UserImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * Home controller providing the main protected endpoints.
 * 
 * These endpoints require authentication via JWT token.
 * Get your JWT token by logging in via: /oauth2/authorization/azure
 */
@RestController
@RequestMapping("/api/v1")
public class HomeController {

    /**
     * Protected home endpoint - requires authentication.
     * 
     * @param principal The authenticated principal (automatically injected by Spring Security)
     * @return Welcome message with authentication status
     */
    @GetMapping("/home")
    public Map<String, Object> home(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to Azure SSO REST API!");
        response.put("authenticated", true);
        response.put("user", principal != null ? principal.getName() : "anonymous");
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    /**
     * Home endpoint with user details from JWT.
     * 
     * @param user The authenticated OAuth2 user
     * @return Welcome message with user details
     */
    @GetMapping("/home/details")
    public Map<String, Object> homeWithDetails(@AuthenticationPrincipal OAuth2UserImpl user) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to Azure SSO REST API!");
        response.put("authenticated", true);
        
        if (user != null && user.getUser() != null) {
            response.put("user", Map.of(
                "id", user.getUser().getId(),
                "email", user.getUser().getEmail(),
                "name", user.getUser().getName(),
                "oid", user.getUser().getOid() != null ? user.getUser().getOid() : "N/A"
            ));
        }
        
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    /**
     * Status endpoint - shows authentication status and details.
     * 
     * @param authentication The authentication object (null if not authenticated)
     * @return Authentication status information
     */
    @GetMapping("/status")
    public Map<String, Object> status(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        if (authentication != null && authentication.isAuthenticated()) {
            response.put("authenticated", true);
            response.put("principal", authentication.getName());
            response.put("authorities", authentication.getAuthorities());
            response.put("principalType", authentication.getPrincipal().getClass().getSimpleName());
            
            // If it's our OAuth2UserImpl, extract additional details
            if (authentication.getPrincipal() instanceof OAuth2UserImpl oauth2User) {
                response.put("userDetails", Map.of(
                    "id", oauth2User.getUser().getId(),
                    "email", oauth2User.getUser().getEmail(),
                    "name", oauth2User.getUser().getName() != null ? oauth2User.getUser().getName() : "N/A"
                ));
            }
        } else {
            response.put("authenticated", false);
            response.put("message", "No valid authentication found");
        }
        
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }
}
