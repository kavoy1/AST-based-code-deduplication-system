package com.ast.back.modules.auth.session;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryRefreshSessionStore {

    private final Map<String, RefreshSession> sessions = new ConcurrentHashMap<>();

    public void save(RefreshSession session) {
        sessions.put(session.sessionId(), session);
    }

    public RefreshSession find(String sessionId) {
        RefreshSession session = sessions.get(sessionId);
        if (session == null) {
            return null;
        }
        if (session.expiresAt().isBefore(Instant.now())) {
            sessions.remove(sessionId);
            return null;
        }
        return session;
    }

    public boolean isActive(String sessionId, Long userId) {
        RefreshSession session = find(sessionId);
        return session != null && session.userId().equals(userId);
    }

    public void rotateRefreshId(String sessionId, String refreshId, Instant expiresAt) {
        RefreshSession current = find(sessionId);
        if (current == null) {
            return;
        }
        save(new RefreshSession(
                current.sessionId(),
                current.userId(),
                current.role(),
                current.username(),
                refreshId,
                expiresAt
        ));
    }

    public void revoke(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            sessions.remove(sessionId);
        }
    }
}
