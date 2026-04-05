package com.example.azuressoapi.service;

import com.example.azuressoapi.dto.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for JWT token generation and validation.
 * Generates custom JWT tokens after successful Azure OAuth2 authentication.
 */
@Service
public class JwtService {

    @Value("${app.security.jwt.secret-key}")
    private String secretKey;

    @Value("${app.security.jwt.expiration:86400000}") // Default 24 hours
    private long expiration;

    /**
     * Generate JWT token for authenticated user.
     *
     * @param user The authenticated user
     * @return JWT token string
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("email", user.getEmail());
        claims.put("name", user.getName());
        claims.put("oid", user.getOid()); // Azure AD Object ID
        claims.put("provider", "AZURE");

        return createToken(claims, user.getEmail());
    }

    /**
     * Validate if token is valid and not expired.
     *
     * @param token JWT token
     * @return true if valid, false otherwise
     */
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract username (email) from token.
     *
     * @param token JWT token
     * @return username/email
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Parse token and extract user information.
     *
     * @param token JWT token
     * @return User object
     */
    public User parseToken(String token) {
        Claims claims = extractAllClaims(token);

        return User.builder()
            .id(claims.get("id", String.class))
            .email(claims.getSubject())
            .name(claims.get("name", String.class))
            .oid(claims.get("oid", String.class))
            .build();
    }

    private boolean isTokenExpired(String token) {
        Date expirationDate = extractExpiration(token);
        return expirationDate.before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSignKey(), Jwts.SIG.HS256)
            .compact();
    }

    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith((SecretKey) getSignKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
