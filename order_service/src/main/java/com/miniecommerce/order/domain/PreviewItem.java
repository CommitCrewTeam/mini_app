package com.miniecommerce.order.domain;

import java.util.Map;

public record PreviewItem(
        String productId,
        int quantity,
        long unitPrice,
        String name,
        Map<String, Object> detail,
        boolean active,
        int stock,
        boolean available) {
}