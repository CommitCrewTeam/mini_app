package com.miniecommerce.notification.app.port.outbound;

import com.miniecommerce.notification.domain.event.OrderPlacedEvent;

public interface SendOrderPlacedNotificationPort {

    String send();

    default String send(OrderPlacedEvent event) {
        return send();
    }
}
