package com.miniecommerce.user.adapter.inbound.rest.dto;

import java.time.LocalDate;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String userId,
        String firstName,
        String lastName,
        String email,
        String address,
        LocalDate birthDate) {
}
