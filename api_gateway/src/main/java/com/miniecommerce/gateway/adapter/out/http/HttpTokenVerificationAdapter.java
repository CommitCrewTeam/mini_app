package com.miniecommerce.gateway.adapter.out.http;

import com.miniecommerce.common.response.ApiResponse;
import com.miniecommerce.gateway.domain.model.TokenVerification;
import com.miniecommerce.gateway.application.port.out.TokenVerificationPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Adapter đầu ra: gọi auth_service qua HTTP và chuyển kết quả wire DTO
 * thành model của domain.
 */
@Component
public class HttpTokenVerificationAdapter implements TokenVerificationPort {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthHttpClient authHttpClient;

    public HttpTokenVerificationAdapter(AuthHttpClient authHttpClient) {
        this.authHttpClient = authHttpClient;
    }

    @Override
    public Mono<TokenVerification> verify(String accessToken) {
        return authHttpClient.verifyToken(BEARER_PREFIX + accessToken)
                .map(this::toDomain);
    }

    private TokenVerification toDomain(ApiResponse<VerifyTokenResponse> response) {
        VerifyTokenResponse data = response == null ? null : response.getData();

        boolean valid = response != null
                && response.isSuccess()
                && data != null
                && data.valid();

        return valid ? new TokenVerification(true) : TokenVerification.invalid();
    }
}
