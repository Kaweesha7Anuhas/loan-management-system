package com.example.loanmanagement.service;

import com.example.loanmanagement.config.CustomUserDetails;
import com.example.loanmanagement.dto.request.LoginRequest;
import com.example.loanmanagement.dto.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authentication orchestration.
 *
 * The critical bit here (and the most common Spring Security 6+ pitfall):
 * after authenticationManager.authenticate(...) succeeds, we must
 * EXPLICITLY save the SecurityContext to the HttpSession. In Spring
 * Security 6+, SecurityContextHolderFilter does NOT auto-save the context
 * back to the session, so without this step the login would appear to
 * succeed (200 + user JSON) but every subsequent request would return 401
 * because the session would be empty.
 *
 * We use HttpSessionSecurityContextRepository directly (not injected) because
 * it's stateless — it just reads/writes the session attribute
 * SPRING_SECURITY_CONTEXT. A fresh instance behaves identically to the one
 * the filter chain uses.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;

    public AuthService(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    /**
     * Authenticate the user, save the session, and return user info.
     *
     * @throws com.example.loanmanagement.exception.ApiException
     *         mapped to 401 if credentials are invalid.
     */
    public UserResponse login(LoginRequest request,
                              HttpServletRequest httpRequest,
                              HttpServletResponse httpResponse) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword())
            );

            // Persist the authenticated context to the session.
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            new HttpSessionSecurityContextRepository()
                    .saveContext(context, httpRequest, httpResponse);

            CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
            log.info("User '{}' logged in successfully", principal.getUsername());
            return toUserResponse(principal);

        } catch (AuthenticationException ex) {
            // Do NOT leak whether the username exists or the password is wrong —
            // a single message for both cases prevents username enumeration.
            log.warn("Failed login attempt for '{}'", request.getUsername());
            throw new com.example.loanmanagement.exception.ApiException(
                    HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    /**
     * Return the currently authenticated user's info.
     * Called from GET /api/auth/me — the filter chain has already populated
     * the Authentication, we just translate it into a DTO.
     */
    public UserResponse getCurrentUser(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new com.example.loanmanagement.exception.ApiException(
                    HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return toUserResponse(principal);
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private UserResponse toUserResponse(CustomUserDetails principal) {
        // Strip the ROLE_ prefix — the API contract exposes "ADMIN",
        // not "ROLE_ADMIN". The prefix is a Spring Security detail.
        Set<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .collect(Collectors.toSet());

        return UserResponse.builder()
                .id(principal.getUserId())
                .username(principal.getUsername())
                .email(principal.getEmail())
                .fullName(principal.getFullName())
                .roles(roles)
                .enabled(principal.isEnabled())
                .build();
    }
}