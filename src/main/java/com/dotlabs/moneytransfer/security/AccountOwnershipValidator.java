package com.dotlabs.moneytransfer.security;

import com.dotlabs.moneytransfer.entity.User;
import com.dotlabs.moneytransfer.enums.Role;
import com.dotlabs.moneytransfer.exception.InvalidTransferException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AccountOwnershipValidator {

    /**
     * Validates that the currently authenticated user owns the source account,
     * or has ROLE_ADMIN privileges.
     */
    public void validateOwnership(String sourceAccountNumber) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // Unauthenticated - will be blocked by Spring Security filter chain or allowed if anonymous direct transfers permitted
            return;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            if (user.getRole() == Role.ROLE_ADMIN) {
                return; // Admin can operate any account
            }
            if (user.getAccountNumber() != null && !user.getAccountNumber().trim().equalsIgnoreCase(sourceAccountNumber.trim())) {
                throw new InvalidTransferException("Unauthorized: You can only transfer funds from your own account ("
                        + user.getAccountNumber() + ")");
            }
        }
    }
}
