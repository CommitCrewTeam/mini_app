package com.miniecommerce.order.app.service;

import com.miniecommerce.order.app.port.in.CreateOrderUseCase;

public class OrderService implements CreateOrderUseCase {

    @Override
    public String placeOrder() {
        return "ORDER_CREATED";
    }
}
