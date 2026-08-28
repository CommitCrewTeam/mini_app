package com.miniecommerce.order.app.port.outbound;

import com.miniecommerce.order.domain.Order;

public interface SaveOrderPort {

    Order save(Order order);
}
