package com.miniecommerce.auth.app.port.inbound;

import com.miniecommerce.auth.app.command.AssignRoleCommand;

public interface AssignRoleUseCase {

    void assign(AssignRoleCommand command);
}
