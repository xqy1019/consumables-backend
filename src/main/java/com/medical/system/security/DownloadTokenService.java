package com.medical.system.security;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DownloadTokenService {

    // token -> username, with expiry tracked by creation time
    private final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();

    private record TokenInfo(String username, long createdAt) {}

    /**
     * Generate a short-lived download token (valid for 60 seconds)
     */
    public String generateToken(String username) {
        // Clean expired tokens
        long now = System.currentTimeMillis();
        tokens.entrySet().removeIf(e -> now - e.getValue().createdAt() > 60_000);

        String token = UUID.randomUUID().toString();
        tokens.put(token, new TokenInfo(username, now));
        return token;
    }

    /**
     * Validate and consume a download token (one-time use)
     */
    public String validateAndConsume(String token) {
        TokenInfo info = tokens.remove(token);
        if (info == null) return null;
        if (System.currentTimeMillis() - info.createdAt() > 60_000) return null;
        return info.username();
    }
}
