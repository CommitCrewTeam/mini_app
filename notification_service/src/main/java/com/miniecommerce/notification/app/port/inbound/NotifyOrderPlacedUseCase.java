package com.miniecommerce.notification.app.port.inbound;

import com.miniecommerce.notification.domain.event.OrderPlacedEvent;

public interface NotifyOrderPlacedUseCase {

    String notifyOrderPlaced();

    default String notifyOrderPlaced(OrderPlacedEvent event) {
        return notifyOrderPlaced();
    }
}
