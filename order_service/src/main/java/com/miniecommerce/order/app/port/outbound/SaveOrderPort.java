package com.miniecommerce.order.app.port.outbound;

import com.miniecommerce.order.domain.OrderAggregateRoot;

public interface SaveOrderPort {

    OrderAggregateRoot save(OrderAggregateRoot order);
}
