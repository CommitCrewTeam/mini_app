package com.miniecommerce.gateway.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniecommerce.gateway.domain.model.AuthenticationResult;
import com.miniecommerce.gateway.domain.model.TokenStatus;
import com.miniecommerce.gateway.domain.port.in.AuthenticateTokenUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthGlobalFilterTest {

    private final AuthenticateTokenUseCase authenticateTokenUseCase = mock(AuthenticateTokenUseCase.class);
    private final GatewayFilterChain chain = mock(GatewayFilterChain.class);
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    private final AuthGlobalFilter filter = new AuthGlobalFilter(
            authenticateTokenUseCase,
            new GatewayErrorResponseWriter(objectMapper)
    );

    @Test
    void allowsPublicPathWithoutAuthentication() {
        MockServerWebExchange exchange = exchangeFor(
                MockServerHttpRequest.post("/api/v1/auth/login").build());
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
        verifyNoInteractions(authenticateTokenUseCase);
    }

    @Test
    void allowsCorsPreflightWithoutAuthentication() {
        MockServerWebExchange exchange = exchangeFor(
                MockServerHttpRequest.method(HttpMethod.OPTIONS, "/api/v1/orders").build());
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
        verifyNoInteractions(authenticateTokenUseCase);
    }

    @Test
    void rejectsRequestWithoutAuthorizationHeader() {
        MockServerWebExchange exchange = exchangeFor(
                MockServerHttpRequest.get("/api/v1/orders").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertUnauthorized(exchange, "Missing or invalid Authorization header");
        verify(chain, never()).filter(any());
        verifyNoInteractions(authenticateTokenUseCase);
    }

    @Test
    void rejectsInvalidToken() {
        when(authenticateTokenUseCase.authenticate("abc"))
                .thenReturn(Mono.just(AuthenticationResult.of(TokenStatus.INVALID)));

        MockServerWebExchange exchange = exchangeFor(
                MockServerHttpRequest.get("/api/v1/orders")
                        .header("Authorization", "Bearer abc")
                        .build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertUnauthorized(exchange, "Invalid or expired token");
        verify(chain, never()).filter(any());
    }

    @Test
    void returnsServiceUnavailableWhenAuthServiceIsDown() {
        when(authenticateTokenUseCase.authenticate("abc"))
                .thenReturn(Mono.just(AuthenticationResult.of(TokenStatus.UNAVAILABLE)));

        MockServerWebExchange exchange = exchangeFor(
                MockServerHttpRequest.get("/api/v1/orders")
                        .header("Authorization", "Bearer abc")
                        .build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
        assertTrue(bodyOf(exchange).contains("Authentication service unavailable"));
        verify(chain, never()).filter(any());
    }

    @Test
    void forwardsRequestWhenTokenIsValid() {
        when(authenticateTokenUseCase.authenticate("abc"))
                .thenReturn(Mono.just(AuthenticationResult.of(TokenStatus.VALID)));
        when(chain.filter(any())).thenReturn(Mono.empty());

        MockServerWebExchange exchange = exchangeFor(
                MockServerHttpRequest.get("/api/v1/orders")
                        .header("Authorization", "bearer abc")
                        .build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
        verify(authenticateTokenUseCase).authenticate("abc");
    }

    private MockServerWebExchange exchangeFor(MockServerHttpRequest request) {
        return MockServerWebExchange.from(request);
    }

    private void assertUnauthorized(MockServerWebExchange exchange, String message) {
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertTrue(bodyOf(exchange).contains(message));
    }

    private String bodyOf(MockServerWebExchange exchange) {
        return exchange.getResponse().getBodyAsString().block();
    }
}
