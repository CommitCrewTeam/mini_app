package com.miniecommerce.gateway.domain.port.in;

import com.miniecommerce.gateway.domain.model.AuthenticationResult;
import reactor.core.publisher.Mono;

/**
 * Input port (driving port): use case xác thực access token của request đi vào gateway.
 * Adapter đầu vào (WebFilter) chỉ được phụ thuộc vào interface này.
 */
public interface AuthenticateTokenUseCase {

    /**
     * @param accessToken token đã được tách khỏi header Authorization (không gồm tiền tố "Bearer ")
     * @return trạng thái xác thực; không bao giờ rỗng
     */
    Mono<AuthenticationResult> authenticate(String accessToken);
}
