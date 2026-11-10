package com.example.loanmanagement.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Wraps a User entity to satisfy Spring Security's UserDetails contract,
 * while also exposing the user's database ID and display name that Spring's
 * built-in UserDetails implementations don't provide.
 *
 * Controllers and services can cast the current Principal to this class:
 *
 *   @AuthenticationPrincipal CustomUserDetails current =
 *       (CustomUserDetails) authentication.getPrincipal();
 *   Long userId = current.getUserId();
 *   String name = current.getFullName();
 *
 * Fields are final and there are no setters — once constructed, an
 * authentication principal is immutable. This prevents accidental
 * tampering mid-request.
 */
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final String fullName;
    private final String email;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Long userId,
                             String username,
                             String password,
                             String fullName,
                             String email,
                             boolean enabled,
                             Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.enabled = enabled;
        this.authorities = authorities;
    }

    // ─────────────────────────────────────────────
    // Custom accessors — extra fields beyond UserDetails
    // ─────────────────────────────────────────────

    public Long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    // ─────────────────────────────────────────────
    // UserDetails contract
    // ─────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Return false to make an account temporarily expire after a defined
     * period. Currently all accounts are non-expiring — return true.
     * If a future feature adds "temporary access", change this to a
     * field-driven check.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Return false to lock an account (e.g. after N failed login attempts).
     * Not implemented here — Admin uses `enabled` for permanent deactivation.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Return false to force a password reset (e.g. credentials expired).
     * Not implemented here — a "must change password" flag would live on
     * the User entity and be checked here.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}