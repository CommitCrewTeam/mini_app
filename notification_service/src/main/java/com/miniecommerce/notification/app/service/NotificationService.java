package com.miniecommerce.notification.app.service;

import com.miniecommerce.notification.app.port.inbound.NotifyOrderPlacedUseCase;
import com.miniecommerce.notification.app.port.outbound.SendOrderPlacedNotificationPort;

public class NotificationService implements NotifyOrderPlacedUseCase {

    private final SendOrderPlacedNotificationPort sendOrderPlacedNotificationPort;

    public NotificationService(SendOrderPlacedNotificationPort sendOrderPlacedNotificationPort) {
        this.sendOrderPlacedNotificationPort = sendOrderPlacedNotificationPort;
    }

    @Override
    public String notifyOrderPlaced() {
        return sendOrderPlacedNotificationPort.send();
    }
}
