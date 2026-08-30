package com.miniecommerce.order.adapter.inbound.rest.dto;

import com.miniecommerce.order.domain.OrderStatus;

import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String id,
        String customerId,
        List<ItemResponse> items,
        long shippingFee,
        long totalAmount,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public record ItemResponse(String productId, int quantity, long unitPrice) {
    }
}