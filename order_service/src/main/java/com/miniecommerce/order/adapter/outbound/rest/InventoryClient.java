package com.miniecommerce.order.adapter.outbound.rest;

import com.miniecommerce.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "inventory-service", url = "${app.inventory.url:http://localhost:8082}")
public interface InventoryClient {

    @GetMapping("/api/phones/{id}")
    ApiResponse<Integer> getStock(@PathVariable("id") Long id);
}
