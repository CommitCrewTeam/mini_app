package com.miniecommerce.order.adapter.outbound.rest;

import com.miniecommerce.common.response.ApiResponse;
import com.miniecommerce.order.adapter.outbound.rest.dto.ShippingMethodDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "shipping-service", url = "${app.shipping.url:http://localhost:8084}")
public interface ShippingClient {

    @GetMapping("/api/v1/shipping-methods")
    ApiResponse<List<ShippingMethodDto>> listActive();
}