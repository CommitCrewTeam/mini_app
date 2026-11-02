package com.miniecommerce.auth.adapter.outbound.persistence.mapper;

import com.miniecommerce.auth.adapter.outbound.persistence.AuthEntity;
import com.miniecommerce.auth.domain.Auth;
import org.springframework.stereotype.Component;

/**
 * Translates between the domain model and the JPA persistence model so that
 * the domain stays free of JPA concerns.
 */
@Component
public class AuthEntityMapper {

    public Auth toDomain(AuthEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Auth(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.isActive(),
                entity.getRoles().stream()
                        .filter(role -> role.isActive())
                        .map(role -> role.getCode())
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()),
                entity.getRoles().stream()
                        .filter(role -> role.isActive())
                        .flatMap(role -> role.getPermissions().stream())
                        .filter(permission -> permission.isActive())
                        .map(permission -> permission.getCode())
                        .collect(java.util.stream.Collectors.toUnmodifiableSet())
        );
    }

    public AuthEntity toEntity(Auth auth) {
        if (auth == null) {
            return null;
        }
        return AuthEntity.builder()
                .id(auth.id())
                .username(auth.username())
                .passwordHash(auth.passwordHash())
                .active(auth.active())
                .build();
    }
}
