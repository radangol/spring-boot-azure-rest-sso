package com.example.azuressoapi.config;

import com.example.azuressoapi.dto.User;
import com.example.azuressoapi.service.JwtService;
import com.example.azuressoapi.service.OAuth2UserImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;

/**
 * JWT Authentication Filter.
 * 
 * This filter intercepts requests and validates JWT tokens in the Authorization header.
 * If a valid token is found, it authenticates the user in the Spring Security context.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Get Authorization header
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Check if header exists and starts with "Bearer "
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token
        String token = header.substring(7).trim();

        try {
            // Validate token
            if (!jwtService.isTokenValid(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Parse user from token
            User user = jwtService.parseToken(token);

            // Create OAuth2UserImpl for compatibility
            OAuth2UserImpl userDetails = new OAuth2UserImpl(user, new HashMap<>(), "email");

            // Create authentication token
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // Log error and continue without authentication
            logger.warn("JWT token validation failed: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
