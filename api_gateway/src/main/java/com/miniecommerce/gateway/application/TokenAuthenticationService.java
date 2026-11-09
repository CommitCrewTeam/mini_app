package com.miniecommerce.gateway.application;

import com.miniecommerce.gateway.domain.model.AuthenticationResult;
import com.miniecommerce.gateway.domain.model.TokenStatus;
import com.miniecommerce.gateway.domain.port.in.AuthenticateTokenUseCase;
import com.miniecommerce.gateway.domain.port.out.TokenVerificationPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Use case xác thực token: điều phối output port và áp dụng chính sách timeout.
 * Không chứa kiến thức về HTTP/Spring Gateway.
 */
@Service
public class TokenAuthenticationService implements AuthenticateTokenUseCase {

    private static final Duration VERIFY_TIMEOUT = Duration.ofSeconds(3);

    private final TokenVerificationPort tokenVerificationPort;

    public TokenAuthenticationService(TokenVerificationPort tokenVerificationPort) {
        this.tokenVerificationPort = tokenVerificationPort;
    }

    @Override
    public Mono<AuthenticationResult> authenticate(String accessToken) {
        return tokenVerificationPort.verify(accessToken)
                .map(verification -> verification.valid()
                        ? AuthenticationResult.of(TokenStatus.VALID)
                        : AuthenticationResult.of(TokenStatus.INVALID))
                .defaultIfEmpty(AuthenticationResult.of(TokenStatus.INVALID))
                .timeout(VERIFY_TIMEOUT)
                // Lỗi hạ tầng (timeout, mất kết nối...) không được làm sập request,
                // mà được biểu diễn thành trạng thái UNAVAILABLE.
                .onErrorResume(exception -> Mono.just(AuthenticationResult.of(TokenStatus.UNAVAILABLE)));
    }
}
