package com.miniecommerce.auth.adapter.inbound.rest;

import com.miniecommerce.auth.adapter.inbound.rest.dto.*;
import com.miniecommerce.auth.adapter.inbound.rest.mapper.AuthRestMapper;
import com.miniecommerce.auth.app.port.inbound.LoginUseCase;
import com.miniecommerce.auth.app.port.inbound.RefreshTokenUseCase;
import com.miniecommerce.auth.app.port.inbound.RegisterUseCase;
import com.miniecommerce.auth.app.result.LoginResult;
import com.miniecommerce.auth.domain.Auth;
import com.miniecommerce.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final AuthRestMapper mapper;
    private final RefreshTokenUseCase refreshTokenUseCase;


    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        Auth auth = registerUseCase.register(mapper.toRegisterCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(mapper.toRegisterResponse(auth), "Register successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = loginUseCase.login(mapper.toLoginCommand(request));
        return ResponseEntity.ok(ApiResponse.success(mapper.toLoginResponse(result), "Login successfully"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        LoginResult result = refreshTokenUseCase.refresh(
                mapper.toRefreshTokenCommand(request));
        return ResponseEntity.ok(
                ApiResponse.success(mapper.toLoginResponse(result), "Refresh successfully"));
    }
}
