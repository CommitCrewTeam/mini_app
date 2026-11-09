package com.miniecommerce.gateway.domain.model;

/**
 * Kết quả của use case xác thực token mà gateway dùng để quyết định
 * cho request đi tiếp hay không.
 */
public record AuthenticationResult(TokenStatus status) {

    public static AuthenticationResult of(TokenStatus status) {
        return new AuthenticationResult(status);
    }
}
