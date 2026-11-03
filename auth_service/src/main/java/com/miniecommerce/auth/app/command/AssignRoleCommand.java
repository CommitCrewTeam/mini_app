package com.miniecommerce.auth.app.command;

import java.util.UUID;

public record AssignRoleCommand(UUID userId, String roleCode) {
}
