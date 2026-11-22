package com.miniecommerce.auth.adapter.inbound.rest.dto;

import java.time.LocalDate;

public record RegisteredUserResponse(
    String firstName, String lastName, String email, String address, LocalDate birthDate) {}
