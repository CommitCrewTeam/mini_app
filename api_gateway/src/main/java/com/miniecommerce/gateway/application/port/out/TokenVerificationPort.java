package com.miniecommerce.gateway.application.port.out;

import com.miniecommerce.gateway.domain.model.TokenVerification;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Output port used by TokenAuthenticationService to verify an access token. */
public interface TokenVerificationPort {

    /**
     * Bypass tạm: trả về empty để ứng dụng khởi động được khi chưa có adapter gọi auth_service.
     * TokenAuthenticationService sẽ map empty -> AuthenticationResult(INVALID).
     */
    default Mono<TokenVerification> verify(String accessToken) {
        return Mono.empty();
    }
}
