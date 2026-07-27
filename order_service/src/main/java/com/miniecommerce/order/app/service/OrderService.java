package com.miniecommerce.order.app.service;

import com.miniecommerce.order.app.port.inbound.CreateOrderUseCase;
import com.miniecommerce.order.app.port.outbound.SaveOrderPort;

public class OrderService implements CreateOrderUseCase {

    private final SaveOrderPort saveOrderPort;

    public OrderService(SaveOrderPort saveOrderPort) {
        this.saveOrderPort = saveOrderPort;
    }

    @Override
    public String placeOrder() {
        return saveOrderPort.save();
    }
}
