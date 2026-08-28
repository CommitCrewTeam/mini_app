package com.miniecommerce.order.app.port.outbound;

public interface InventoryPort {

    int getStock(Long phoneId);
}
