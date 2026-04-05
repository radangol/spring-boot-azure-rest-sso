package com.example.azuressoapi.service;

import com.example.azuressoapi.dto.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Custom OAuth2 User Service for Azure AD authentication.
 * 
 * This service handles user information after successful Azure AD authentication.
 * It extracts user details from the OAuth2 response and creates/updates user records.
 */
@Service
@RequiredArgsConstructor
public class AzureOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Extract Azure AD user attributes
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String givenName = oAuth2User.getAttribute("given_name");
        String familyName = oAuth2User.getAttribute("family_name");
        String oid = oAuth2User.getAttribute("oid"); // Azure AD Object ID
        String preferredUsername = oAuth2User.getAttribute("preferred_username");
        String tenantId = oAuth2User.getAttribute("tid");

        // Create User DTO
        User user = User.builder()
            .id(oid) // Using Azure AD Object ID as user ID
            .oid(oid)
            .email(email != null ? email : preferredUsername)
            .name(name)
            .givenName(givenName)
            .familyName(familyName)
            .preferredUsername(preferredUsername)
            .tenantId(tenantId)
            .provider("AZURE")
            .build();

        // Determine the name attribute key
        String nameAttributeKey = userRequest.getClientRegistration()
            .getProviderDetails()
            .getUserInfoEndpoint()
            .getUserNameAttributeName();
        
        if (nameAttributeKey == null || nameAttributeKey.isEmpty()) {
            nameAttributeKey = "sub"; // Default to 'sub' (standard OIDC claim)
        }

        // Return wrapped OAuth2User with our User DTO
        return new OAuth2UserImpl(user, oAuth2User.getAttributes(), nameAttributeKey);
    }
}
