package com.miniecommerce.notification.app.service;

import com.miniecommerce.notification.app.port.inbound.NotifyOrderPlacedUseCase;
import com.miniecommerce.notification.app.port.outbound.SendOrderPlacedNotificationPort;
import com.miniecommerce.notification.domain.event.OrderPlacedEvent;
import org.springframework.stereotype.Service;

@Service
public class NotificationService implements NotifyOrderPlacedUseCase {

    private final SendOrderPlacedNotificationPort sendOrderPlacedNotificationPort;

    public NotificationService(SendOrderPlacedNotificationPort sendOrderPlacedNotificationPort) {
        this.sendOrderPlacedNotificationPort = sendOrderPlacedNotificationPort;
    }

    @Override
    public String notifyOrderPlaced() {
        return sendOrderPlacedNotificationPort.send();
    }

    @Override
    public String notifyOrderPlaced(OrderPlacedEvent event) {
        return sendOrderPlacedNotificationPort.send(event);
    }
}
