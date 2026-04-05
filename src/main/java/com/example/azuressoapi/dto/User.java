package com.example.azuressoapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User DTO representing authenticated user information.
 * Used for both Azure OAuth2 authentication and JWT token claims.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    /**
     * Internal user ID (can be same as OID for Azure users)
     */
    private String id;
    
    /**
     * Azure AD Object ID - unique identifier for the user in Azure AD
     */
    private String oid;
    
    /**
     * User's email address
     */
    private String email;
    
    /**
     * User's display name
     */
    private String name;
    
    /**
     * User's given name (first name)
     */
    private String givenName;
    
    /**
     * User's family name (last name)
     */
    private String familyName;
    
    /**
     * Azure AD tenant ID
     */
    private String tenantId;
    
    /**
     * Preferred username (usually email)
     */
    private String preferredUsername;
    
    /**
     * Authentication provider (AZURE in this case)
     */
    private String provider;
}
