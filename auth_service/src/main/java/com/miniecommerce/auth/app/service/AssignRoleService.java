package com.miniecommerce.auth.app.service;

import com.miniecommerce.auth.app.command.AssignRoleCommand;
import com.miniecommerce.auth.app.port.inbound.AssignRoleUseCase;
import com.miniecommerce.auth.app.port.outbound.RoleAssignmentRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssignRoleService implements AssignRoleUseCase {

    private final RoleAssignmentRepositoryPort roleAssignmentRepository;

    @Override
    public void assign(AssignRoleCommand command) {
        roleAssignmentRepository.assignRole(command.userId(), command.roleCode());
    }
}
