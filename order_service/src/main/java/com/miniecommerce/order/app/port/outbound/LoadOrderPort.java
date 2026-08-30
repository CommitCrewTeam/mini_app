package com.miniecommerce.order.app.port.outbound;

import com.miniecommerce.order.domain.OrderAggregateRoot;

import java.util.Optional;

public interface LoadOrderPort {

    Optional<OrderAggregateRoot> findById(String orderId);
}