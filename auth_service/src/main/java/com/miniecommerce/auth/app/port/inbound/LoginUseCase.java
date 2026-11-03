package com.miniecommerce.auth.app.port.inbound;

import com.miniecommerce.auth.app.command.LoginCommand;
import com.miniecommerce.auth.app.result.LoginResult;

public interface LoginUseCase {

    LoginResult login(LoginCommand command);
}
