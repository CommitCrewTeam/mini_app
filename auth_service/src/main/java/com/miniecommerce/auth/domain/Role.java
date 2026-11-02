package com.miniecommerce.auth.domain;

import java.util.UUID;

/**
 * A named collection of permissions used by the authorization layer.
 * Examples: ROLE_ADMIN and ROLE_CUSTOMER.
 */
public record Role(
        UUID id,
        String code,
        String name,
        String description,
        boolean active
) {

    public Role activate() {
        return new Role(id, code, name, description, true);
    }

    public Role deactivate() {
        return new Role(id, code, name, description, false);
    }
}
