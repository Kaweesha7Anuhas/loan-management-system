package com.example.loanmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * User info returned to the frontend on login and via GET /api/auth/me.
 *
 * Deliberately EXCLUDES the password hash. We never return it, not even
 * the hash — it leaks information (algorithm, cost, salt timing) and is
 * useless to the client.
 *
 * Roles are returned WITHOUT the ROLE_ prefix — "ADMIN" not "ROLE_ADMIN" —
 * because the frontend uses these strings for UI hints (show the "Approve"
 * button to ADMIN), and the prefix is a Spring Security implementation
 * detail that shouldn't leak into the API contract.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Set<String> roles;
    private boolean enabled;
}