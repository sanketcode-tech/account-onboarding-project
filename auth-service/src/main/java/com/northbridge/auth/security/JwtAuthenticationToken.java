package com.northbridge.auth.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * JWT Authentication Token
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;

    private final Object principal;
    private final Long userId;

    public JwtAuthenticationToken(Object principal, Long userId, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.userId = userId;
        setAuthenticated(true);
    }

    public JwtAuthenticationToken(Object principal, Long userId, Collection<? extends GrantedAuthority> authorities, Object details) {
        super(authorities);
        this.principal = principal;
        this.userId = userId;
        setDetails(details);
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public Long getUserId() {
        return userId;
    }
}

