package com.miniecommerce.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Locale;

public enum ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    CONFLICT(HttpStatus.CONFLICT),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
    BAD_GATEWAY(HttpStatus.BAD_GATEWAY);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Resolves an error code that may come from a downstream service as either
     * an enum name ("CONFLICT") or an HTTP status as text ("409").
     */
    public static ErrorCode from(String code) {
        return from(code, INTERNAL_ERROR);
    }

    /**
     * Same as {@link #from(String)} but returns {@code fallback} when the code
     * is blank, unknown, or not a valid number.
     */
    public static ErrorCode from(String code, ErrorCode fallback) {
        if (code == null || code.isBlank()) {
            return fallback;
        }

        String normalized = code.trim();

        if (normalized.chars().allMatch(Character::isDigit)) {
            try {
                return fromStatus(Integer.parseInt(normalized), fallback);
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        try {
            return valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public static ErrorCode fromStatus(int status) {
        return fromStatus(status, status >= 500 ? INTERNAL_ERROR : BAD_REQUEST);
    }

    public static ErrorCode fromStatus(int status, ErrorCode fallback) {
        return switch (status) {
            case 400 -> BAD_REQUEST;
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 409 -> CONFLICT;
            case 502, 503, 504 -> BAD_GATEWAY;
            default -> fallback;
        };
    }
}
