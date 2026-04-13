package com.ast.back.modules.auth.session;

import java.time.Instant;

public record RefreshSession(
        String sessionId,
        Long userId,
        String role,
        String username,
        String currentRefreshId,
        Instant expiresAt
) {
}
