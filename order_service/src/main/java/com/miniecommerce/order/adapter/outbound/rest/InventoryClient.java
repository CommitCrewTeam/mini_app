package com.miniecommerce.order.adapter.outbound.rest;

import com.miniecommerce.common.response.ApiResponse;
import com.miniecommerce.order.adapter.outbound.rest.dto.PhoneDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "inventory-service", url = "${app.inventory.url:http://localhost:8082}")
public interface InventoryClient {

    @GetMapping("/api/phones/{id}")
    ApiResponse<Integer> getStock(@PathVariable("id") String id);

    @GetMapping("/api/phones/by-ids")
    ApiResponse<List<PhoneDto>> getItemsForOrderPreview(@RequestParam("ids") List<Long> ids);
}