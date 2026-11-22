package com.miniecommerce.auth.app.port.outbound;

import java.util.Optional;

import com.miniecommerce.auth.domain.RefreshToken;

public interface RefreshTokenRepositoryPort {

  RefreshToken save(RefreshToken refreshToken);

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  int removeByTokenHash(String tokenHash);
}
