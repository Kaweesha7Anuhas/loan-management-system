package com.example.loanmanagement.config;

import com.example.loanmanagement.entity.User;
import com.example.loanmanagement.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security's UserDetailsService implementation — the bridge between
 * our MySQL `users` table and Spring Security's authentication mechanism.
 *
 * Called by Spring Security's DaoAuthenticationProvider during:
 *  - Form login (default page from Boot)
 *  - Custom POST /api/auth/login (Phase 4.3, once AuthenticationManager exists)
 *  - Any request where a valid session must be re-hydrated (Spring reuses
 *    the cached SecurityContext in the session — it does NOT call us on
 *    every request; that's only on login).
 *
 * The username parameter is named `usernameOrEmail` because the spec allows
 * login by either. We look up by username first, then fall back to email.
 *
 * @Transactional(readOnly = true) — ensures lazy roles are initializable
 * inside this method even if the calling context is not transactional.
 * With @EntityGraph on the repository, roles are already loaded in the
 * single query, so this is belt-and-braces.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + usernameOrEmail));

        // Build Spring Security authorities. IMPORTANT: prefix "ROLE_"
        // because hasRole('ADMIN') silently prepends it — storing "ADMIN"
        // without the prefix would break every hasRole() check.
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet());

        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getFullName(),
                user.getEmail(),
                Boolean.TRUE.equals(user.getEnabled()),
                authorities
        );
    }
}