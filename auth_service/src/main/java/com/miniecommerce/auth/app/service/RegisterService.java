package com.miniecommerce.auth.app.service;

import com.miniecommerce.auth.app.command.RegisterCommand;
import com.miniecommerce.auth.app.port.inbound.RegisterUseCase;
import com.miniecommerce.auth.app.port.outbound.AuthRepositoryPort;
import com.miniecommerce.auth.app.port.outbound.PasswordHasherPort;
import com.miniecommerce.auth.domain.Auth;
import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterService implements RegisterUseCase {

    private final AuthRepositoryPort authRepository;
    private final PasswordHasherPort passwordHasher;

    @Override
    public Auth register(RegisterCommand command) {
        String username = command.username().trim().toLowerCase();
        if (username.length() < 4 || username.length() > 100) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Username must be 4-100 characters");
        }
        if (authRepository.existsByUsername(username)) {
            throw new AppException(ErrorCode.CONFLICT, "Username already exists");
        }
        Auth newAuth = new Auth(null, username, passwordHasher.hash(command.rawPassword()), true);
        return authRepository.save(newAuth);
    }
}
