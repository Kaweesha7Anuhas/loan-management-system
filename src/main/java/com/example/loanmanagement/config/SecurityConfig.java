package com.example.loanmanagement.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Spring Security configuration. Final production-ready state.
 *
 * Security Highlights:
 *  - Session-based auth (JSESSIONID cookie)
 *  - CSRF ENABLED for all state-changing requests (POST, PUT, PATCH, DELETE)
 *  - The frontend (api.js) automatically reads the XSRF-TOKEN cookie and attaches
 *    it as the X-XSRF-TOKEN header, satisfying the CSRF requirement.
 *  - Only /api/auth/login and /api/auth/csrf are exempt from CSRF (pre-auth).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);

        http
                // ─────────────────────────────────────────
                // CSRF: Strictly enforced everywhere except login endpoints
                // ─────────────────────────────────────────
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepo)
                        .csrfTokenRequestHandler(csrfHandler)
                        .ignoringRequestMatchers(
                                "/api/auth/login",
                                "/api/auth/csrf"
                        )
                )

                // ─────────────────────────────────────────
                // Authorization rules
                // ─────────────────────────────────────────
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/index.html", "/login.html",
                                "/css/**", "/js/**", "/images/**",
                                "/favicon.ico", "/error"
                        ).permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/csrf").permitAll()
                        .anyRequest().authenticated()
                )

                // ─────────────────────────────────────────
                // Logout — JSON response, session invalidated
                // ─────────────────────────────────────────
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .logoutSuccessHandler((req, res, authentication) -> {
                            res.setStatus(HttpServletResponse.SC_OK);
                            res.setContentType("application/json");
                            res.getWriter().write(
                                    "{\"success\":true,\"message\":\"Logged out successfully\"}");
                        })
                )

                // ─────────────────────────────────────────
                // JSON 401/403 handlers
                // ─────────────────────────────────────────
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint())
                        .accessDeniedHandler(jsonAccessDeniedHandler())
                )

                // ─────────────────────────────────────────
                // Session policy
                // ─────────────────────────────────────────
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                );

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint jsonAuthenticationEntryPoint() {
        return (request, response, ex) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":401,\"error\":\"Unauthorized\","
                    + "\"message\":\"Authentication required\","
                    + "\"path\":\"" + request.getRequestURI() + "\"}");
        };
    }

    @Bean
    public AccessDeniedHandler jsonAccessDeniedHandler() {
        return (request, response, ex) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":403,\"error\":\"Forbidden\","
                    + "\"message\":\"You do not have permission to perform this action\","
                    + "\"path\":\"" + request.getRequestURI() + "\"}");
        };
    }
}