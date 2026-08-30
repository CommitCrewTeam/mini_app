package com.miniecommerce.order.app.port.outbound;

import com.miniecommerce.order.domain.event.OrderPlacedEvent;

public interface PublishOrderEventPort {

    void publishOrderCreated(OrderPlacedEvent event);
}