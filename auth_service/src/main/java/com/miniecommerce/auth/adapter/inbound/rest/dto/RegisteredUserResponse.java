package com.miniecommerce.auth.adapter.inbound.rest.dto;

import java.time.LocalDate;
import java.util.UUID;

public record RegisteredUserResponse(
        String firstName,
        String lastName,
        String email,
        String address,
        LocalDate birthDate
) {
}
