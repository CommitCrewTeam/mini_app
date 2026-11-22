package com.miniecommerce.gateway.adapter.out.http;

/**
 * DTO ánh xạ payload của auth_service. Thuộc adapter hạ tầng, không rò rỉ vào domain.
 */
public record VerifyTokenResponse(boolean valid) {
}
