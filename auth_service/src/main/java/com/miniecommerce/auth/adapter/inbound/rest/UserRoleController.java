package com.miniecommerce.auth.adapter.inbound.rest;

import com.miniecommerce.auth.adapter.inbound.rest.dto.AddRoleRequest;
import com.miniecommerce.auth.app.command.AssignRoleCommand;
import com.miniecommerce.auth.app.port.inbound.AssignRoleUseCase;
import com.miniecommerce.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserRoleController {

    private final AssignRoleUseCase assignRoleUseCase;

    @PostMapping("/{userId}/roles")
    @PreAuthorize("(hasRole('SUPER_ADMIN') and #p1.roleCode == 'ROLE_ADMIN')"
            + " or (hasRole('ADMIN') and #p1.roleCode == 'ROLE_STAFF')")
    public ResponseEntity<ApiResponse<Void>> addRole(
            @PathVariable UUID userId,
            @Valid @RequestBody AddRoleRequest request) {
        assignRoleUseCase.assign(new AssignRoleCommand(userId, request.roleCode()));
        return ResponseEntity.ok(ApiResponse.success(null, "Role assigned successfully"));
    }
}
