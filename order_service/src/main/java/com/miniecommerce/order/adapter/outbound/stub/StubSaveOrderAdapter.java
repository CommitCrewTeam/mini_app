package com.miniecommerce.order.adapter.outbound.stub;

import com.miniecommerce.order.app.port.outbound.SaveOrderPort;
import com.miniecommerce.order.domain.Order;
import org.springframework.stereotype.Component;

@Component
public class StubSaveOrderAdapter implements SaveOrderPort {

    @Override
    public Order save(Order order) {
        order.setId(1L);
        return order;
    }
}
