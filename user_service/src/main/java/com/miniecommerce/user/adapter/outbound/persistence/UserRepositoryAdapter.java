package com.miniecommerce.user.adapter.outbound.persistence;

import com.miniecommerce.user.adapter.outbound.persistence.mapper.UserPersistenceMapper;
import com.miniecommerce.user.app.port.outbound.UserRepository;
import com.miniecommerce.user.domain.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserNeo4jRepository neo4jRepository;
    private final UserPersistenceMapper mapper;

    public UserRepositoryAdapter(UserNeo4jRepository neo4jRepository, UserPersistenceMapper mapper) {
        this.neo4jRepository = neo4jRepository;
        this.mapper = mapper;
    }

    @Override
    public List<User> findAll() {
        return neo4jRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<User> findById(UUID id) {
        return neo4jRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity saved = neo4jRepository.save(mapper.toEntity(user));
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsByUserId(String userId) {
        return neo4jRepository.existsByUserId(userId);
    }

    @Override
    public boolean existsByEmail(String email) {
        return neo4jRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByUserIdAndIdNot(String userId, UUID id) {
        return neo4jRepository.existsByUserIdAndIdNot(userId, id);
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, UUID id) {
        return neo4jRepository.existsByEmailAndIdNot(email, id);
    }
}
