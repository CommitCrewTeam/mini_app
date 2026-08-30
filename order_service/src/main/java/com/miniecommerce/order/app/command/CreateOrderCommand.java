package com.miniecommerce.order.app.command;

import java.util.List;

public record CreateOrderCommand(String customerId, long shippingFee, List<Item> items) {

    public record Item(String productId, int quantity, long unitPrice) {
    }
}