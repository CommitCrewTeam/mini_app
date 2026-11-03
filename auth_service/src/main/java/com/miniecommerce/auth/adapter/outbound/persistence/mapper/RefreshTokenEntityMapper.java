package com.miniecommerce.auth.adapter.outbound.persistence.mapper;

import com.miniecommerce.auth.adapter.outbound.persistence.RefreshTokenEntity;
import com.miniecommerce.auth.domain.RefreshToken;
import org.springframework.stereotype.Component;

/**
 * Translates between the domain model and the JPA persistence model so that
 * the domain stays free of JPA concerns.
 */
@Component
public class RefreshTokenEntityMapper {

    public RefreshToken toDomain(RefreshTokenEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RefreshToken(
                entity.getId(),
                entity.getAccountId(),
                entity.getTokenHash(),
                entity.getExpiresAt()
        );
    }

    public RefreshTokenEntity toEntity(RefreshToken refreshToken) {
        if (refreshToken == null) {
            return null;
        }
        return RefreshTokenEntity.builder()
                .id(refreshToken.id())
                .accountId(refreshToken.accountId())
                .tokenHash(refreshToken.tokenHash())
                .expiresAt(refreshToken.expiresAt())
                .build();
    }
}
