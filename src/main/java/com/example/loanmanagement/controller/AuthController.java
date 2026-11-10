package com.example.loanmanagement.controller;

import com.example.loanmanagement.dto.request.LoginRequest;
import com.example.loanmanagement.dto.response.ApiResponse;
import com.example.loanmanagement.dto.response.UserResponse;
import com.example.loanmanagement.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST authentication endpoints.
 *
 *  POST /api/auth/login     → authenticate + establish session (CSRF-exempt)
 *  GET  /api/auth/me        → return current user (requires session)
 *  GET  /api/auth/csrf      → return a CSRF token for the frontend (CSRF-exempt)
 *
 * Logout is handled by Spring Security's own filter
 * (POST /api/auth/logout — see SecurityConfig), not by this controller,
 * because the logout DSL cleanly handles session invalidation + cookie
 * deletion + the JSON response handler.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        UserResponse user = authService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.success("Login successful", user));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(authService.getCurrentUser(authentication)));
    }

    /**
     * Returns the current CSRF token. Needed because:
     *  - The login endpoint is CSRF-exempt (pre-auth requests can't have
     *    a token yet).
     *  - But POST /logout and every other state-changing endpoint requires
     *    a CSRF token in the X-XSRF-TOKEN header.
     *  - So the frontend calls GET /api/auth/csrf once, reads the token,
     *    and includes it on every subsequent POST/PUT/PATCH/DELETE.
     *
     * Spring Security injects the CsrfToken argument automatically because
     * CsrfFilter populates it as a request attribute.
     */
    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<Map<String, String>>> csrf(CsrfToken token) {
        return ResponseEntity.ok(
                ApiResponse.success(Map.of("token", token.getToken())));
    }
}