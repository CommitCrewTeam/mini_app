package com.miniecommerce.notification.adapter.outbound.stub;

import com.miniecommerce.notification.app.port.outbound.SendOrderPlacedNotificationPort;
import org.springframework.stereotype.Component;

@Component
public class StubSendOrderPlacedNotificationAdapter implements SendOrderPlacedNotificationPort {

    @Override
    public String send() {
        return "ORDER_PLACED_NOTIFICATION_SENT_BY_STUB_ADAPTER";
    }
}
