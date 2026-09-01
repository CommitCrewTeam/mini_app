package com.miniecommerce.shipping.adapter.inbound.rest;

import com.miniecommerce.common.response.ApiResponse;
import com.miniecommerce.shipping.app.port.inbound.ListShippingMethodsUseCase;
import com.miniecommerce.shipping.domain.ShippingMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shipping-methods")
public class ShippingMethodController {

    private final ListShippingMethodsUseCase listShippingMethodsUseCase;

    public ShippingMethodController(ListShippingMethodsUseCase listShippingMethodsUseCase) {
        this.listShippingMethodsUseCase = listShippingMethodsUseCase;
    }

    @GetMapping
    public ApiResponse<List<ShippingMethod>> listActive() {
        return ApiResponse.success(listShippingMethodsUseCase.listActive());
    }
}