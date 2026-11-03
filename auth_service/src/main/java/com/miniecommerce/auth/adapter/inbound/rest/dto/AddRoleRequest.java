package com.miniecommerce.auth.adapter.inbound.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddRoleRequest(
        @NotBlank
        @Pattern(regexp = "ROLE_ADMIN|ROLE_STAFF", message = "roleCode must be ROLE_ADMIN or ROLE_STAFF")
        String roleCode
) {
}
