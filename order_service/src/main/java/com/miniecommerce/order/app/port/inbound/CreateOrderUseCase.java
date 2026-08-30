package com.miniecommerce.order.app.port.inbound;

import com.miniecommerce.order.domain.OrderAggregateRoot;

public interface CreateOrderUseCase {

    OrderAggregateRoot placeOrder(OrderAggregateRoot order);
}
