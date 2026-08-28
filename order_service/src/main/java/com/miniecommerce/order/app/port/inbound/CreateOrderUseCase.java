package com.miniecommerce.order.app.port.inbound;

import com.miniecommerce.order.domain.Order;

public interface CreateOrderUseCase {

    Order placeOrder(Order order);
}
