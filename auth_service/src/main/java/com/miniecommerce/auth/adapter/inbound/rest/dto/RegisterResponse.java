package com.miniecommerce.auth.adapter.inbound.rest.dto;


import java.util.UUID;

public record RegisterResponse(UUID id, String username, boolean active) {}