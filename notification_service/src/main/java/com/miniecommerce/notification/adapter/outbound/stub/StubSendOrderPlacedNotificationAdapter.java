package com.miniecommerce.notification.adapter.outbound.stub;

import com.miniecommerce.notification.app.port.outbound.SendOrderPlacedNotificationPort;
import com.miniecommerce.notification.domain.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StubSendOrderPlacedNotificationAdapter implements SendOrderPlacedNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(StubSendOrderPlacedNotificationAdapter.class);

    @Override
    public String send() {
        return "ORDER_PLACED_NOTIFICATION_SENT_BY_STUB_ADAPTER";
    }

    @Override
    public String send(OrderPlacedEvent event) {
        log.info("Sending order placed notification for orderId={}, customerId={}, totalAmount={}",
                event.orderId(), event.customerId(), event.totalAmount());
        return "ORDER_PLACED_NOTIFICATION_SENT_BY_STUB_ADAPTER";
    }
}
