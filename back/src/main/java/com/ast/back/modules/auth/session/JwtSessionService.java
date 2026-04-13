package com.ast.back.modules.auth.session;

import com.ast.back.modules.user.persistence.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtSessionService {

    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_SESSION_ID = "sid";
    private static final String CLAIM_REFRESH_ID = "rid";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final AuthSessionProperties properties;
    private final InMemoryRefreshSessionStore refreshSessionStore;
    private SecretKey signingKey;

    public JwtSessionService(AuthSessionProperties properties, InMemoryRefreshSessionStore refreshSessionStore) {
        this.properties = properties;
        this.refreshSessionStore = refreshSessionStore;
    }

    @PostConstruct
    void init() {
        this.signingKey = Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public AuthTokens createSession(User user) {
        Instant now = Instant.now();
        String sessionId = UUID.randomUUID().toString();
        String refreshId = UUID.randomUUID().toString();
        Instant refreshExpiresAt = now.plus(Duration.ofDays(properties.getRefreshTokenDays()));
        refreshSessionStore.save(new RefreshSession(
                sessionId,
                user.getId(),
                user.getRole(),
                user.getUsername(),
                refreshId,
                refreshExpiresAt
        ));
        return buildTokens(user.getId(), user.getUsername(), user.getRole(), sessionId, refreshId, now, refreshExpiresAt);
    }

    public AuthTokens refreshSession(String refreshToken) {
        Claims claims = parseAndValidate(refreshToken, REFRESH_TOKEN_TYPE);
        Long userId = Long.valueOf(claims.getSubject());
        String sessionId = claims.get(CLAIM_SESSION_ID, String.class);
        String refreshId = claims.get(CLAIM_REFRESH_ID, String.class);
        RefreshSession session = refreshSessionStore.find(sessionId);
        if (session == null || !session.userId().equals(userId) || !session.currentRefreshId().equals(refreshId)) {
            throw new UnauthorizedException("刷新令牌无效或已失效");
        }

        Instant now = Instant.now();
        String nextRefreshId = UUID.randomUUID().toString();
        Instant refreshExpiresAt = now.plus(Duration.ofDays(properties.getRefreshTokenDays()));
        refreshSessionStore.rotateRefreshId(sessionId, nextRefreshId, refreshExpiresAt);
        return buildTokens(userId, session.username(), session.role(), sessionId, nextRefreshId, now, refreshExpiresAt);
    }

    public AuthenticatedUser authenticateAccessToken(String accessToken) {
        Claims claims = parseAndValidate(accessToken, ACCESS_TOKEN_TYPE);
        Long userId = Long.valueOf(claims.getSubject());
        String sessionId = claims.get(CLAIM_SESSION_ID, String.class);
        if (!refreshSessionStore.isActive(sessionId, userId)) {
            throw new UnauthorizedException("会话已失效，请重新登录");
        }
        return new AuthenticatedUser(
                userId,
                claims.get(CLAIM_USERNAME, String.class),
                claims.get(CLAIM_ROLE, String.class),
                sessionId
        );
    }

    public void logout(String accessToken, String refreshToken) {
        String sessionId = extractSessionId(accessToken, ACCESS_TOKEN_TYPE);
        if (sessionId == null) {
            sessionId = extractSessionId(refreshToken, REFRESH_TOKEN_TYPE);
        }
        refreshSessionStore.revoke(sessionId);
    }

    public AuthSessionProperties getProperties() {
        return properties;
    }

    private AuthTokens buildTokens(Long userId, String username, String role, String sessionId, String refreshId, Instant now, Instant refreshExpiresAt) {
        Instant accessExpiresAt = now.plus(Duration.ofMinutes(properties.getAccessTokenMinutes()));
        String accessToken = Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_TYPE, ACCESS_TOKEN_TYPE)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_SESSION_ID, sessionId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(accessExpiresAt))
                .signWith(signingKey)
                .compact();

        String refreshToken = Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_TYPE, REFRESH_TOKEN_TYPE)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_SESSION_ID, sessionId)
                .claim(CLAIM_REFRESH_ID, refreshId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(refreshExpiresAt))
                .signWith(signingKey)
                .compact();

        return new AuthTokens(accessToken, refreshToken, accessExpiresAt, refreshExpiresAt, sessionId);
    }

    private Claims parseAndValidate(String token, String expectedType) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("缺少令牌");
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new UnauthorizedException("令牌类型不正确");
            }
            return claims;
        } catch (ExpiredJwtException e) {
            throw new UnauthorizedException("令牌已过期");
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("令牌无效");
        }
    }

    private String extractSessionId(String token, String expectedType) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return parseAndValidate(token, expectedType).get(CLAIM_SESSION_ID, String.class);
        } catch (UnauthorizedException e) {
            return null;
        }
    }
}
