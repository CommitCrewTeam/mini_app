package com.miniecommerce.order.domain.event;

import com.miniecommerce.order.domain.OrderStatus;

import java.time.Instant;
import java.util.List;

public record OrderPlacedEvent(
        String eventId,
        String orderId,
        String customerId,
        List<Item> items,
        long shippingFee,
        long totalAmount,
        OrderStatus status,
        Instant occurredAt) {

    public record Item(String productId, int quantity, long unitPrice) {
    }
}