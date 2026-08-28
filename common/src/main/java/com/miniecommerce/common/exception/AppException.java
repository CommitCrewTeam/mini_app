package com.miniecommerce.common.exception;

import org.springframework.http.HttpStatus;

public class AppException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public AppException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
