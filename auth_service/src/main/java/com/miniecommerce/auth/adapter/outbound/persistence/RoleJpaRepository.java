package com.miniecommerce.auth.adapter.outbound.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, UUID> {

  Optional<RoleEntity> findByCode(String code);
}
