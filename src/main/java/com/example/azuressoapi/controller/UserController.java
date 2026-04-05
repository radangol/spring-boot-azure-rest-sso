package com.example.azuressoapi.controller;

import com.example.azuressoapi.dto.User;
import com.example.azuressoapi.service.OAuth2UserImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * User controller for retrieving authenticated user profile information.
 * 
 * This controller extracts user information from the JWT token.
 */
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    /**
     * Get the authenticated user's profile information.
     * 
     * Extracts information from the authenticated OAuth2 user (JWT token).
     * 
     * @param oauth2User The OAuth2 user (automatically injected by Spring Security)
     * @return User profile information
     */
    @GetMapping("/profile")
    public Map<String, Object> getUserProfile(@AuthenticationPrincipal OAuth2UserImpl oauth2User) {
        Map<String, Object> profile = new HashMap<>();
        
        if (oauth2User != null && oauth2User.getUser() != null) {
            User user = oauth2User.getUser();
            
            profile.put("id", user.getId());
            profile.put("oid", user.getOid());
            profile.put("email", user.getEmail());
            profile.put("name", user.getName());
            profile.put("givenName", user.getGivenName());
            profile.put("familyName", user.getFamilyName());
            profile.put("preferredUsername", user.getPreferredUsername());
            profile.put("tenantId", user.getTenantId());
            profile.put("provider", user.getProvider());
        } else {
            profile.put("error", "No authenticated user found");
        }
        
        return profile;
    }

    /**
     * Get all OAuth2 attributes from the authenticated user.
     * Useful for debugging and understanding what claims are available.
     * 
     * @param oauth2User The OAuth2 user
     * @return All attributes from the OAuth2 user
     */
    @GetMapping("/claims")
    public Map<String, Object> getAllClaims(@AuthenticationPrincipal OAuth2UserImpl oauth2User) {
        Map<String, Object> response = new HashMap<>();
        
        if (oauth2User != null) {
            response.put("attributes", oauth2User.getAttributes());
            response.put("name", oauth2User.getName());
            response.put("username", oauth2User.getUsername());
            response.put("authorities", oauth2User.getAuthorities());
        } else {
            response.put("error", "No authenticated user found");
        }
        
        return response;
    }

    /**
     * Get current user information (alias for profile).
     * 
     * @param oauth2User The OAuth2 user
     * @return User information
     */
    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(@AuthenticationPrincipal OAuth2UserImpl oauth2User) {
        return getUserProfile(oauth2User);
    }
}
