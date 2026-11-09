package com.miniecommerce.gateway.domain.model;

/**
 * Kết quả xác thực một access token do auth_service trả về.
 * Đây là model thuần của domain, không phụ thuộc vào DTO/HTTP.
 */
public record TokenVerification(boolean valid) {

    public static TokenVerification invalid() {
        return new TokenVerification(false);
    }
}
