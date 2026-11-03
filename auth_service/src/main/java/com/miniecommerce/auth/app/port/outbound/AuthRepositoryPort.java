package com.miniecommerce.auth.app.port.outbound;

import com.miniecommerce.auth.domain.Auth;

import java.util.Optional;
import java.util.UUID;

public interface AuthRepositoryPort {
    boolean existsByUsername(String username);

    Optional<Auth> findByUsername(String username);

    Auth save(Auth auth);

    Optional<Auth> findById(UUID id);
}