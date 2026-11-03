package com.miniecommerce.auth.adapter.outbound.persistence;

import com.miniecommerce.auth.adapter.outbound.persistence.mapper.RefreshTokenEntityMapper;
import com.miniecommerce.auth.app.port.outbound.RefreshTokenRepositoryPort;
import com.miniecommerce.auth.domain.RefreshToken;
import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenPersistenceAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository repository;
    private final RefreshTokenEntityMapper mapper;

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        try {
            RefreshTokenEntity saved = repository.save(mapper.toEntity(refreshToken));
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.CONFLICT, "Refresh token already exists");
        }
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public int removeByTokenHash(String tokenHash) {
        return repository.deleteByTokenHash(tokenHash);
    }
}
