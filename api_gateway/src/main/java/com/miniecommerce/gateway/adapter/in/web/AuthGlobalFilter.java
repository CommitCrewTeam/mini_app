package com.miniecommerce.gateway.adapter.in.web;

import com.miniecommerce.gateway.domain.model.AuthenticationResult;
import com.miniecommerce.gateway.domain.port.in.AuthenticateTokenUseCase;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Adapter đầu vào (driving adapter): chặn request tại gateway, chuyển việc
 * xác thực token cho input port {@link AuthenticateTokenUseCase}.
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    // Chỉ những API này mới được public. Không dùng prefix /api/v1/auth/
    // để tránh vô tình public các endpoint thêm sau này như /auth/me.
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh"
    );

    private final AuthenticateTokenUseCase authenticateTokenUseCase;
    private final GatewayErrorResponseWriter errorResponseWriter;

    public AuthGlobalFilter(
            AuthenticateTokenUseCase authenticateTokenUseCase,
            GatewayErrorResponseWriter errorResponseWriter
    ) {
        this.authenticateTokenUseCase = authenticateTokenUseCase;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (isPublicRequest(request)) {
            return chain.filter(exchange);
        }

        String token = getToken(request);
        if (token == null) {
            return errorResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED,
                    "Missing or invalid Authorization header");
        }

        return authenticateTokenUseCase.authenticate(token)
                .flatMap(result -> switch (result.status()) {
                    case VALID -> chain.filter(exchange);
                    case INVALID -> errorResponseWriter.write(exchange, HttpStatus.UNAUTHORIZED,
                            "Invalid or expired token");
                    case UNAVAILABLE -> errorResponseWriter.write(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                            "Authentication service unavailable");
                });
    }

    private boolean isPublicRequest(ServerHttpRequest request) {
        // CORS preflight request không mang token.
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return true;
        }

        String path = request.getURI().getPath();
        return PUBLIC_PATHS.contains(path)
                || path.equals("/actuator")
                || path.startsWith("/actuator/");
    }

    /**
     * Lấy token từ header Authorization: Bearer <token>.
     */
    private String getToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authorization == null
                || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
