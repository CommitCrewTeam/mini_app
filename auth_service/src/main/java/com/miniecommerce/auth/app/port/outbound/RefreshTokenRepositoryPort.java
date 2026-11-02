package com.miniecommerce.auth.app.port.outbound;

import com.miniecommerce.auth.domain.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepositoryPort {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    int removeByTokenHash(String tokenHash);
}
