package com.miniecommerce.auth.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthJpaRepository extends JpaRepository<AuthEntity, UUID> {
    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<AuthEntity> findByUsername(String username);

    @Override
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<AuthEntity> findById(UUID id);
}
