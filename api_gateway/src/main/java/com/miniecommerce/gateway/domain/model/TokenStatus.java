package com.miniecommerce.gateway.domain.model;

/**
 * Trạng thái xác thực token mà gateway cần để quyết định cho request đi tiếp hay không.
 */
public enum TokenStatus {

    VALID,
    INVALID,
    UNAVAILABLE
}
