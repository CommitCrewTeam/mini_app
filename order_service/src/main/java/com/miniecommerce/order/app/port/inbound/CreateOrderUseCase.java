package com.miniecommerce.order.app.port.inbound;

import com.miniecommerce.order.app.command.CreateOrderCommand;
import com.miniecommerce.order.domain.OrderAggregateRoot;

public interface CreateOrderUseCase {

    OrderAggregateRoot placeOrder(CreateOrderCommand command);
}