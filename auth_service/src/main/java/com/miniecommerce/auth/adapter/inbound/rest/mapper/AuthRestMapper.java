package com.miniecommerce.auth.adapter.inbound.rest.mapper;

import com.miniecommerce.auth.adapter.inbound.rest.dto.*;
import com.miniecommerce.auth.app.command.LoginCommand;
import com.miniecommerce.auth.app.command.RefreshTokenCommand;
import com.miniecommerce.auth.app.command.RegisterCommand;
import com.miniecommerce.auth.app.result.LoginResult;
import com.miniecommerce.auth.domain.Auth;
import org.springframework.stereotype.Component;

@Component
public class AuthRestMapper {

    public RegisterCommand toRegisterCommand(RegisterRequest request) {
        return new RegisterCommand(request.username(), request.password());
    }

    public RegisterResponse toRegisterResponse(Auth auth) {
        return new RegisterResponse(auth.id(), auth.username(), auth.active());
    }

    public LoginCommand toLoginCommand(LoginRequest request) {
        return new LoginCommand(request.username(), request.password());
    }

    public LoginResponse toLoginResponse(LoginResult result) {
        return new LoginResponse(
                result.accessToken(),
                "Bearer",
                result.auth().id(),
                result.auth().username(),
                result.refreshToken()
        );
    }

    public RefreshTokenCommand toRefreshTokenCommand(RefreshTokenRequest request) {
        return new RefreshTokenCommand(request.refreshToken());
    }
}
