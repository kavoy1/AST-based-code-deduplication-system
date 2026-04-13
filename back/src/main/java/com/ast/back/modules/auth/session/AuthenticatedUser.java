package com.ast.back.modules.auth.session;

public record AuthenticatedUser(
        Long userId,
        String username,
        String role,
        String sessionId
) {
}
