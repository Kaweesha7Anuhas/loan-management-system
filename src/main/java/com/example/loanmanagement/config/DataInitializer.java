package com.example.loanmanagement.config;

import com.example.loanmanagement.entity.Role;
import com.example.loanmanagement.entity.User;
import com.example.loanmanagement.enums.RoleName;
import com.example.loanmanagement.repository.RoleRepository;
import com.example.loanmanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Seeds the three system roles and one default user per role on first startup.
 *
 * Design notes:
 *  - IDEMPOTENT: checks existsByName / existsByUsername before each insert.
 *    Safe to run on every application restart without side effects. If a
 *    password was changed via the app, it will NOT be reset to the default.
 *  - TRANSACTIONAL: all seeding writes commit together. If one insert fails,
 *    the whole thing rolls back — no partial state.
 *  - DEV BOOTSTRAP ONLY: the credentials logged here are for development. In
 *    a real deployment the initial admin is created via a one-time CLI tool
 *    or an environment-variable-injected initial password, then changed on
 *    first login. This class is a controlled development convenience, not a
 *    production mechanism.
 *  - The three role names match RoleName enum values: ADMIN, LOAN_OFFICER,
 *    CASHIER. Spring Security authorities will be constructed as
 *    "ROLE_" + these names by CustomUserDetailsService.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // ─────────────────────────────────────────
        // Step 1 — seed roles (never changes)
        // ─────────────────────────────────────────
        Role adminRole       = ensureRole(RoleName.ADMIN,        "System administrator with full access");
        Role loanOfficerRole = ensureRole(RoleName.LOAN_OFFICER, "Loan officer — customer and application management");
        Role cashierRole     = ensureRole(RoleName.CASHIER,      "Cashier — repayment recording");

        // ─────────────────────────────────────────
        // Step 2 — seed default users (one per role)
        // ─────────────────────────────────────────
        boolean adminSeeded = ensureUser(
                "admin", "Admin@123", "System Administrator",
                "admin@lms.local", adminRole);

        boolean officerSeeded = ensureUser(
                "officer1", "Officer@123", "Loan Officer One",
                "officer1@lms.local", loanOfficerRole);

        boolean cashierSeeded = ensureUser(
                "cashier1", "Cashier@123", "Cashier One",
                "cashier1@lms.local", cashierRole);

        // ─────────────────────────────────────────
        // Step 3 — tell the developer what happened
        // ─────────────────────────────────────────
        if (adminSeeded || officerSeeded || cashierSeeded) {
            log.info("\n" +
                    "=================================================================\n" +
                    "  LMS — Initial Data Seeded\n" +
                    "-----------------------------------------------------------------\n" +
                    "  ADMIN        : admin     / Admin@123\n" +
                    "  LOAN_OFFICER : officer1  / Officer@123\n" +
                    "  CASHIER      : cashier1  / Cashier@123\n" +
                    "  ** These are DEVELOPMENT credentials. Change before deployment. **\n" +
                    "=================================================================");
        } else {
            log.info("Initial data verified — all default users already present.");
        }
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private Role ensureRole(RoleName roleName, String description) {
        return roleRepository.findByName(roleName.name())
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(roleName.name())
                                .description(description)
                                .build()
                ));
    }

    /**
     * Creates the user if not present. Returns true if a NEW user was created
     * (so we know whether to log the credential block).
     */
    private boolean ensureUser(String username, String plainPassword,
                               String fullName, String email, Role role) {

        if (userRepository.existsByUsername(username)) {
            return false;
        }

        Set<Role> roles = new HashSet<>();
        roles.add(role);

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(plainPassword))
                .email(email)
                .fullName(fullName)
                .enabled(true)
                .roles(roles)
                .build();

        userRepository.save(user);
        return true;
    }
}