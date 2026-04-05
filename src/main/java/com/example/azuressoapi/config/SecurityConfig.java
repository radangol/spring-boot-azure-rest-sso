package com.example.azuressoapi.config;

import com.example.azuressoapi.service.AzureOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for Azure AD OAuth2 authentication.
 * 
 * This configuration enables:
 * 1. OAuth2 Login with Azure AD (browser-based flow)
 * 2. Custom JWT token generation after successful OAuth2 login
 * 3. JWT-based authentication for API endpoints
 * 4. Stateless session management
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AzureOAuth2UserService azureOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    /**
     * Configure security filter chain for REST API endpoints.
     * 
     * Flow:
     * 1. User hits /oauth2/authorization/azure -> redirects to Azure AD login
     * 2. Azure redirects back to /login/oauth2/code/azure
     * 3. OAuth2LoginSuccessHandler generates and returns JWT token
     * 4. Client uses JWT token for subsequent API requests
     * 5. JwtAuthFilter validates token on each request
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Configure authorization rules
            .authorizeHttpRequests(authorize -> authorize
                // Allow unauthenticated access to health check and OAuth2 login endpoints
                .requestMatchers("/", "/login", "/actuator/health", "/login/oauth2/code/**").permitAll()
                
                // Require authentication for all /api/** endpoints
                .requestMatchers("/api/**").authenticated()
                
                // Require authentication for all other requests
                .anyRequest().authenticated()
            )
            
            // Configure OAuth2 Login for Azure AD
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(azureOAuth2UserService)
                )
                .successHandler(oAuth2LoginSuccessHandler)
            )
            
            // Add JWT authentication filter
            .addFilterBefore(jwtAuthFilter, BasicAuthenticationFilter.class)
            
            // Disable CSRF for stateless API
            .csrf(csrf -> csrf.disable())
            
            // Stateless session - no server-side session storage
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(restAuthenticationEntryPoint))
            
            // Enable CORS
            .cors(Customizer.withDefaults());
        
        return http.build();
    }

    /**
     * CORS configuration for allowing cross-origin requests.
     * 
     * Uncomment and configure if your API will be called from a browser-based frontend
     * running on a different domain (e.g., React/Angular SPA).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow requests from these origins (customize for your frontend)
        // configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:4200"));
        configuration.setAllowedOriginPatterns(Arrays.asList("*")); // For development only
        
        // Allow these HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Allow these headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Expose these headers to the client
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        
        // Cache preflight requests for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
