package com.miniecommerce.gateway.adapter.in.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniecommerce.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Ghi response lỗi theo format ApiResponse thống nhất của hệ thống.
 * Tách khỏi filter để filter chỉ tập trung vào luồng xử lý request,
 * và dùng chung contract response với GlobalExceptionHandler ở module common.
 */
@Component
public class GatewayErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public GatewayErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] body = serialize(ApiResponse.error(String.valueOf(status.value()), message));

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body))
        );
    }

    private byte[] serialize(ApiResponse<Void> response) {
        try {
            return objectMapper.writeValueAsBytes(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize gateway error response", exception);
        }
    }
}
