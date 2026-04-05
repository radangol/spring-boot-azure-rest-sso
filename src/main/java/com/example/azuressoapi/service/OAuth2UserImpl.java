package com.example.azuressoapi.service;

import com.example.azuressoapi.dto.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Implementation of OAuth2User that wraps our User DTO.
 * This class bridges Spring Security's OAuth2User interface with our custom User model.
 */
@Getter
public class OAuth2UserImpl implements OAuth2User, UserDetails {
    
    private final User user;
    private final Map<String, Object> attributes;
    private final String nameAttributeKey;

    public OAuth2UserImpl(User user, Map<String, Object> attributes, String nameAttributeKey) {
        this.user = user;
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
    }

    // OAuth2User methods
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        if (nameAttributeKey != null && attributes != null && attributes.containsKey(nameAttributeKey)) {
            Object name = attributes.get(nameAttributeKey);
            return name != null ? String.valueOf(name) : user.getEmail();
        }
        return user.getEmail();
    }

    // UserDetails methods
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Default user role - can be extended based on your requirements
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return null; // No password for OAuth2 users
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
