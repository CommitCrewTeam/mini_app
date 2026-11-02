package com.miniecommerce.auth.app.service;

import com.miniecommerce.auth.app.command.LoginCommand;
import com.miniecommerce.auth.app.port.inbound.LoginUseCase;
import com.miniecommerce.auth.app.port.outbound.*;
import com.miniecommerce.auth.app.result.LoginResult;
import com.miniecommerce.auth.config.RefreshTokenProperties;
import com.miniecommerce.auth.domain.Auth;
import com.miniecommerce.auth.domain.RefreshToken;
import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LoginService implements LoginUseCase {

    private final AuthRepositoryPort authRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenIssuerPort tokenIssuer;
    private final RefreshTokenProperties properties;
    private final RefreshTokenGeneratorPort tokenGenerator;
    private final RefreshTokenRepositoryPort refreshTokenRepository;

    @Override
    public LoginResult login(LoginCommand command) {
        String username = command.username().trim().toLowerCase();

        Auth auth = authRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Invalid username or password"));

        if (!auth.active() || !passwordHasher.matches(command.rawPassword(), auth.passwordHash())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Invalid username or password");
        }

        String rawRefreshToken = tokenGenerator.generate();
        Instant refreshExpiresAt = Instant.now().plus(properties.ttl());

        RefreshToken newToken = new RefreshToken(
                null,
                auth.id(),
                tokenGenerator.hash(rawRefreshToken),
                refreshExpiresAt
        );
        refreshTokenRepository.save(newToken);

        TokenIssuerPort.IssuedToken token = tokenIssuer.issueAccessToken(auth);

        return new LoginResult(auth, token.value(), rawRefreshToken);
    }
}
