package com.miniecommerce.order.adapter.outbound.rest;

import com.miniecommerce.common.response.ApiResponse;
import com.miniecommerce.order.app.port.outbound.InventoryPort;
import org.springframework.stereotype.Component;

@Component
public class InventoryFeignAdapter implements InventoryPort {

    private final InventoryClient inventoryClient;

    public InventoryFeignAdapter(InventoryClient inventoryClient) {
        this.inventoryClient = inventoryClient;
    }

    @Override
    public int getStock(Long phoneId) {
        ApiResponse<Integer> response = inventoryClient.getStock(phoneId);
        Integer stock = response.getData();
        return stock == null ? 0 : stock;
    }
}
