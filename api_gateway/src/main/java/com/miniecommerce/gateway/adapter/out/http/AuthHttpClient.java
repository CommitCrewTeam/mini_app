package com.miniecommerce.gateway.adapter.out.http;

import com.miniecommerce.common.response.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

/**
 * Mô tả hợp đồng HTTP của auth_service (internal API).
 * Chỉ adapter hạ tầng được biết tới interface này.
 */
public interface AuthHttpClient {

    @PostExchange(url = "/internal/auth/verify")
    Mono<ApiResponse<VerifyTokenResponse>> verifyToken(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    );
}
