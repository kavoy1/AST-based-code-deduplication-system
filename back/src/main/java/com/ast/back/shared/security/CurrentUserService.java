package com.ast.back.shared.security;

import com.ast.back.modules.auth.session.AuthenticatedUser;
import com.ast.back.modules.auth.session.ForbiddenException;

public interface CurrentUserService {

    AuthenticatedUser getCurrentUser();

    default Long getCurrentUserId() {
        return getCurrentUser().userId();
    }

    default String getCurrentUserRole() {
        return getCurrentUser().role();
    }

    default void requireRole(String expectedRole) {
        String currentRole = getCurrentUserRole();
        if (currentRole == null || !currentRole.equalsIgnoreCase(expectedRole)) {
            throw new ForbiddenException("无权限访问");
        }
    }
}
