package com.miniecommerce.user.adapter.outbound.persistence;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.UUID;

public interface UserNeo4jRepository extends Neo4jRepository<UserEntity, UUID> {

    boolean existsByUserId(String userId);

    boolean existsByEmail(String email);

    boolean existsByUserIdAndIdNot(String userId, UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
