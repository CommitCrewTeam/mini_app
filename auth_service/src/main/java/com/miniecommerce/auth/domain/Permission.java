package com.miniecommerce.auth.domain;

import java.util.UUID;

/**
 * One atomic authorization capability.
 * The code is intended for policy checks, for example ORDER_READ.
 */
public record Permission(
        UUID id,
        String code,
        String name,
        String description,
        boolean active
) {

    public Permission activate() {
        return new Permission(id, code, name, description, true);
    }

    public Permission deactivate() {
        return new Permission(id, code, name, description, false);
    }
}
