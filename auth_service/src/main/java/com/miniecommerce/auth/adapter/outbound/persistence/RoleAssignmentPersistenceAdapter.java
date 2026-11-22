package com.miniecommerce.auth.adapter.outbound.persistence;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.miniecommerce.auth.app.port.outbound.RoleAssignmentRepositoryPort;
import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RoleAssignmentPersistenceAdapter implements RoleAssignmentRepositoryPort {

  private final AuthJpaRepository authRepository;
  private final RoleJpaRepository roleRepository;

  @Override
  @Transactional
  public void assignRole(UUID userId, String roleCode) {
    AuthEntity user =
        authRepository
            .findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found"));
    RoleEntity role =
        roleRepository
            .findByCode(roleCode)
            .filter(RoleEntity::isActive)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Role not found or inactive"));

    role.getUsers().add(user);
    user.getRoles().add(role);
  }
}
