package com.miniecommerce.order.adapter.inbound.rest.dto;

import java.util.List;

public record OrderRequest(String customerId, long shippingFee, List<OrderItemRequest> items) {

    public record OrderItemRequest(String productId, int quantity, long unitPrice) {
    }
}