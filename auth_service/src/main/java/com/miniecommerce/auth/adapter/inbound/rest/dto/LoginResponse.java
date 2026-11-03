package com.miniecommerce.auth.adapter.inbound.rest.dto;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        UUID userId,
        String username,
        String refreshToken
) {
}
