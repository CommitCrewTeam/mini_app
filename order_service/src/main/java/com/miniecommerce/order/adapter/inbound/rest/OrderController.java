package com.miniecommerce.order.adapter.inbound.rest;

import com.miniecommerce.order.app.port.inbound.CreateOrderUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }

    @PostMapping("/orders")
    public String placeOrder() {
        return createOrderUseCase.placeOrder();
    }
}
