package com.ipld11stars.backend.security;

import com.ipld11stars.backend.exception.BusinessRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reads the authenticated user's internal id (the JWT subject) from the security context.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static String requireId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BusinessRuleException(HttpStatus.UNAUTHORIZED, "User authentication required");
        }
        return auth.getName();
    }
}
