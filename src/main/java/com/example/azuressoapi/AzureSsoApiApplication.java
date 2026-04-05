package com.example.azuressoapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Azure SSO REST API.
 * 
 * This Spring Boot application demonstrates Azure AD integration for authentication
 * using OAuth2 and OpenID Connect. It provides REST API endpoints protected by JWT tokens.
 */
@SpringBootApplication
public class AzureSsoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AzureSsoApiApplication.class, args);
    }
}
