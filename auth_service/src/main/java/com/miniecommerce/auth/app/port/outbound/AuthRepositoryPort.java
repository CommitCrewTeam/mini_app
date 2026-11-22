package com.miniecommerce.auth.app.port.outbound;

import java.util.Optional;
import java.util.UUID;

import com.miniecommerce.auth.domain.Auth;

public interface AuthRepositoryPort {
  boolean existsByUsername(String username);

  Optional<Auth> findByUsername(String username);

  Auth save(Auth auth);

  Optional<Auth> findById(UUID id);
}
