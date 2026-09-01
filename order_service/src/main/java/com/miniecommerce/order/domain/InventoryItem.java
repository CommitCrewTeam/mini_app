package com.miniecommerce.order.domain;

import java.util.Map;

public record InventoryItem(String productId, String name, Map<String, Object> detail, boolean active, int stock) {

    public boolean isAvailable(int quantity) {
        return active && stock >= quantity;
    }
}