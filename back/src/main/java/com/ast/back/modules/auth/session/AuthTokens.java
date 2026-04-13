package com.ast.back.modules.auth.session;

import java.time.Instant;

public record AuthTokens(
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt,
        String sessionId
) {
}
