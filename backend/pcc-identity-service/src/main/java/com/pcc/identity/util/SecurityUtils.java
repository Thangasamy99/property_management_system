package com.pcc.identity.util;

import com.pcc.identity.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UserPrincipal> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static Long currentUserId() {
        return currentUser().map(UserPrincipal::getId).orElse(null);
    }

    public static String currentUsername() {
        return currentUser().map(UserPrincipal::getUsername).orElse("system");
    }
}
