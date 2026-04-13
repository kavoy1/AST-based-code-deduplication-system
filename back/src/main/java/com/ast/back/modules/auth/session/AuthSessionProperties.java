package com.ast.back.modules.auth.session;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthSessionProperties {

    private String jwtSecret = "ast-dual-token-secret-key-please-change-in-production-2026";
    private long accessTokenMinutes = 15;
    private long refreshTokenDays = 7;
    private String refreshCookieName = "refreshToken";
    private boolean refreshCookieSecure = false;
}
