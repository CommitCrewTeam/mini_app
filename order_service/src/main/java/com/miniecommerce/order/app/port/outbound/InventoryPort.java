package com.miniecommerce.order.app.port.outbound;

import com.miniecommerce.order.domain.InventoryItem;

import java.util.List;

public interface InventoryPort {

    int getStock(String productId);

    List<InventoryItem> getItemsForOrderPreview(List<String> productIds);
}