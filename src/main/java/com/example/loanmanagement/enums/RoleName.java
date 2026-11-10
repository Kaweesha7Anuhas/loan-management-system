package com.example.loanmanagement.enums;

/**
 * The three system roles. Used to seed the roles table and to
 * assign roles during user provisioning.
 *
 * Note: the Role entity stores the name as a String (for future
 * extensibility), but constants here guarantee type safety at
 * compile time in code that references a role.
 *
 * Spring Security expects authorities prefixed with ROLE_,
 * e.g. "ROLE_ADMIN", "ROLE_LOAN_OFFICER", "ROLE_CASHIER".
 */
public enum RoleName {
    ADMIN,
    LOAN_OFFICER,
    CASHIER
}