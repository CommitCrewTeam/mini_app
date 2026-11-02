package com.miniecommerce.auth.app.port.outbound;

import java.util.UUID;

public interface RoleAssignmentRepositoryPort {

    void assignRole(UUID userId, String roleCode);
}
