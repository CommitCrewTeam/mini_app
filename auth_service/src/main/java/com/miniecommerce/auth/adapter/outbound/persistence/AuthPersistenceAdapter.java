package com.miniecommerce.auth.adapter.outbound.persistence;

import com.miniecommerce.auth.adapter.outbound.persistence.mapper.AuthEntityMapper;
import com.miniecommerce.auth.app.port.outbound.AuthRepositoryPort;
import com.miniecommerce.auth.domain.Auth;
import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthPersistenceAdapter implements AuthRepositoryPort {

    private final AuthJpaRepository repository;
    private final AuthEntityMapper mapper;

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    @Override
    public Optional<Auth> findByUsername(String username) {
        return repository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public Auth save(Auth auth) {
        try {
            AuthEntity saved = repository.save(mapper.toEntity(auth));
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.CONFLICT, "Username already exists");
        }
    }

    @Override
    public Optional<Auth> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}