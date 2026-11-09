package com.miniecommerce.user.app.port.outbound;

import com.miniecommerce.user.domain.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    List<User> findAll();

    Optional<User> findById(UUID id);

    User save(User user);

    boolean existsByUserId(String userId);

    boolean existsByEmail(String email);

    boolean existsByUserIdAndIdNot(String userId, UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
