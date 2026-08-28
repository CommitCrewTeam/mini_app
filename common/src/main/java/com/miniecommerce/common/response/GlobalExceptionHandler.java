package com.miniecommerce.common.response;

import com.miniecommerce.common.exception.AppException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error("INTERNAL_ERROR", "Something went wrong"));
    }
}
