package com.miniecommerce.auth.app.port.inbound;

import com.miniecommerce.auth.app.command.RegisterCommand;
import com.miniecommerce.auth.domain.Auth;

public interface RegisterUseCase {
    Auth register(RegisterCommand command);
}