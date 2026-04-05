package com.example.azuressoapi.config;

import com.example.azuressoapi.dto.User;
import com.example.azuressoapi.service.JwtService;
import com.example.azuressoapi.service.OAuth2UserImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom success handler for OAuth2 login.
 * 
 * After successful Azure AD authentication, this handler:
 * 1. Extracts user information from the OAuth2 principal
 * 2. Generates a custom JWT token
 * 3. Returns the JWT token in the response
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        Map<String, Object> attributes = oidcUser.getAttributes();

        User user = new User();
        user.setEmail((String) attributes.get("email"));
        user.setName((String) attributes.get("name"));
        user.setOid((String) attributes.get("oid"));
        // You might need to adjust how you get the ID, depending on your user model
        // For example, if the OID is the ID:
        user.setId(user.getOid());

        // Generate JWT token
        String token = jwtService.generateToken(user);

        // Prepare response
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("token", token);
        responseData.put("user", Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "name", user.getName(),
            "oid", user.getOid()
        ));
        responseData.put("message", "Authentication successful");

        // Set response headers and body
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Authorization", "Bearer " + token);
        response.setStatus(HttpServletResponse.SC_OK);

        // Write JSON response
        response.getWriter().write(objectMapper.writeValueAsString(responseData));
        response.getWriter().flush();
    }
}
