package com.miniecommerce.product.app.port.dto;

import java.io.Serializable;

public record ProductResponse(
        boolean success,
        String message,
        Integer remainingStock,
        Integer totalRequests,
        Integer successfulOrders,
        Integer oversoldAmount,
        Long executionTimeMs,
        String mode
) implements Serializable {
}
