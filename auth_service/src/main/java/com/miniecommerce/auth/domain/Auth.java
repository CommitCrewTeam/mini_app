package com.miniecommerce.auth.domain;

import java.util.Set;
import java.util.UUID;

/**
 * Credential aggregate of the auth service.
 * Only the password hash is stored here, never the raw password.
 */
public record Auth(
        UUID id,
        String username,
        String passwordHash,
        boolean active,
        Set<String> roles,
        Set<String> permissions
) {

    public Auth {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public Auth(UUID id, String username, String passwordHash, boolean active) {
        this(id, username, passwordHash, active, Set.of(), Set.of());
    }

    public Auth deactivate() {
        return new Auth(id, username, passwordHash, false, roles, permissions);
    }

    public Auth activate() {
        return new Auth(id, username, passwordHash, true, roles, permissions);
    }
}
