package com.miniecommerce.notification.domain.event;

import java.time.Instant;
import java.util.List;

public record OrderPlacedEvent(
        String eventId,
        String orderId,
        String customerId,
        List<Item> items,
        long shippingFee,
        long totalAmount,
        String status,
        Instant occurredAt) {

    public record Item(String productId, int quantity, long unitPrice) {
    }
}
