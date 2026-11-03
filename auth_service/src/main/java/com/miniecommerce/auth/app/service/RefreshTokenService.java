package com.miniecommerce.auth.app.service;

import com.miniecommerce.auth.app.command.RefreshTokenCommand;
import com.miniecommerce.auth.app.port.inbound.RefreshTokenUseCase;
import com.miniecommerce.auth.app.port.outbound.AuthRepositoryPort;
import com.miniecommerce.auth.app.port.outbound.RefreshTokenGeneratorPort;
import com.miniecommerce.auth.app.port.outbound.RefreshTokenRepositoryPort;
import com.miniecommerce.auth.app.port.outbound.TokenIssuerPort;
import com.miniecommerce.auth.app.result.LoginResult;
import com.miniecommerce.auth.config.RefreshTokenProperties;
import com.miniecommerce.auth.domain.Auth;
import com.miniecommerce.auth.domain.RefreshToken;
import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenService implements RefreshTokenUseCase {

    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final RefreshTokenGeneratorPort tokenGenerator;
    private final AuthRepositoryPort authRepository;
    private final TokenIssuerPort tokenIssuer;


    @Override
    @Transactional
    public LoginResult refresh(RefreshTokenCommand command) {
        String tokenHash = tokenGenerator.hash(command.refreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AppException(
                        ErrorCode.UNAUTHORIZED, "Invalid refresh token"));

        if (stored.expiresAt().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Invalid refresh token");
        }

        Auth auth = authRepository.findById(stored.accountId())
                .filter(Auth::active)
                .orElseThrow(() -> new AppException(
                        ErrorCode.UNAUTHORIZED, "Invalid refresh token"));

        int deletedTokens = refreshTokenRepository.removeByTokenHash(tokenHash);
        if (deletedTokens != 1) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Invalid refresh token");
        }

        String rawRefreshToken = tokenGenerator.generate();
        Instant refreshExpiresAt = stored.expiresAt();

        RefreshToken newToken = new RefreshToken(
                null,
                auth.id(),
                tokenGenerator.hash(rawRefreshToken),
                refreshExpiresAt
        );
        refreshTokenRepository.save(newToken);

        TokenIssuerPort.IssuedToken accessToken = tokenIssuer.issueAccessToken(auth);

        return new LoginResult(
                auth,
                accessToken.value(),
                rawRefreshToken
        );
    }
}
