package com.miniecommerce.order.adapter.outbound.rest;

import com.miniecommerce.common.response.ApiResponse;
import com.miniecommerce.order.adapter.outbound.rest.dto.PhoneDto;
import com.miniecommerce.order.app.port.outbound.InventoryPort;
import com.miniecommerce.order.domain.InventoryItem;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class InventoryFeignAdapter implements InventoryPort {

    private final InventoryClient inventoryClient;

    public InventoryFeignAdapter(InventoryClient inventoryClient) {
        this.inventoryClient = inventoryClient;
    }

    @Override
    public int getStock(String productId) {
        ApiResponse<Integer> response = inventoryClient.getStock(productId);
        Integer stock = response.getData();
        return stock == null ? 0 : stock;
    }

    @Override
    public java.util.List<InventoryItem> getItemsForOrderPreview(java.util.List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<Long> longIds = productIds.stream()
                .map(this::toLong)
                .filter(java.util.Objects::nonNull)
                .toList();
        ApiResponse<java.util.List<PhoneDto>> response = inventoryClient.getItemsForOrderPreview(longIds);
        Map<Long, PhoneDto> byId = new LinkedHashMap<>();
        if (response != null && response.getData() != null) {
            response.getData().forEach(phone -> byId.put(phone.id(), phone));
        }
        java.util.List<InventoryItem> result = new java.util.ArrayList<>(productIds.size());
        for (String productId : productIds) {
            Long longId = toLong(productId);
            PhoneDto phone = longId == null ? null : byId.get(longId);
            result.add(new InventoryItem(
                    productId,
                    phone == null ? null : phone.name(),
                    phone == null ? null : phone.detail(),
                    phone != null && phone.active(),
                    phone == null ? 0 : phone.stock()));
        }
        return result;
    }

    private Long toLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}