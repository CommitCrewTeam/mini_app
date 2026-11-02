package com.miniecommerce.auth.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token aggregate of the auth service.
 * Only the token hash is persisted, never the raw token.
 * A token that is used, revoked, or expired is deleted. This aggregate keeps no history.
 */
public record RefreshToken(
        UUID id,
        UUID accountId,
        String tokenHash,
        Instant expiresAt
) {

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isActive(Instant now) {
        return !isExpired(now);
    }
}
