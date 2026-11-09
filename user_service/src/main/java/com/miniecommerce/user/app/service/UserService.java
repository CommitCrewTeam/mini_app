package com.miniecommerce.user.app.service;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.user.app.port.inbound.CreateUserUseCase;
import com.miniecommerce.user.app.port.inbound.GetUserUseCase;
import com.miniecommerce.user.app.port.inbound.ListUsersUseCase;
import com.miniecommerce.user.app.port.inbound.UpdateUserUseCase;
import com.miniecommerce.user.app.port.outbound.UserRepository;
import com.miniecommerce.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService implements ListUsersUseCase, CreateUserUseCase, GetUserUseCase, UpdateUserUseCase {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> listAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found"));
    }

    @Override
    @Transactional
    public User createUser(User user) {
        if (repository.existsByUserId(user.userId())) {
            throw new AppException(ErrorCode.CONFLICT, "userId already exists");
        }
        if (repository.existsByEmail(user.email())) {
            throw new AppException(ErrorCode.CONFLICT, "email already exists");
        }
        return repository.save(user);
    }

    @Override
    @Transactional
    public User updateUser(UUID id, User user) {
        repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found"));
        if (repository.existsByUserIdAndIdNot(user.userId(), id)) {
            throw new AppException(ErrorCode.CONFLICT, "userId already exists");
        }
        if (repository.existsByEmailAndIdNot(user.email(), id)) {
            throw new AppException(ErrorCode.CONFLICT, "email already exists");
        }
        User updated = new User(
                id,
                user.userId(),
                user.firstName(),
                user.lastName(),
                user.email(),
                user.address(),
                user.birthDate());
        return repository.save(updated);
    }
}
