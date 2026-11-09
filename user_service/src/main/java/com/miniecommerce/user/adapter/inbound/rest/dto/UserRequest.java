package com.miniecommerce.user.adapter.inbound.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record UserRequest(
        @NotBlank(message = "userId must not be blank") String userId,
        @NotBlank(message = "firstName must not be blank") String firstName,
        @NotBlank(message = "lastName must not be blank") String lastName,
        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address") String email,
        String address,
        LocalDate birthDate) {
}
