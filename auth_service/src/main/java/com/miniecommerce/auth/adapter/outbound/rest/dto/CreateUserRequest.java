package com.miniecommerce.auth.adapter.outbound.rest.dto;

import java.time.LocalDate;

public record CreateUserRequest(
        String userId,
        String firstName,
        String lastName,
        String email,
        String address,
        LocalDate birthDate
) {}
