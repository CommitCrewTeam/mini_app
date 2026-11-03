package com.miniecommerce.auth.adapter.inbound.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 4, max = 100) String username,
        @NotBlank @Size(min = 8, max = 72) String password
) {}