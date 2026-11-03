package com.miniecommerce.auth.adapter.inbound.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(@NotBlank String refreshToken) {
}