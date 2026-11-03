package com.miniecommerce.auth.app.port.inbound;

import com.miniecommerce.auth.app.command.RefreshTokenCommand;
import com.miniecommerce.auth.app.result.LoginResult;

public interface RefreshTokenUseCase {
    public LoginResult refresh(RefreshTokenCommand command);
}
