package com.miniecommerce.notification.infra.kafka;

import com.miniecommerce.notification.application.NotifyOrderPlacedUseCase;
import com.miniecommerce.notification.domain.OrderPlacedNotification;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderPlacedListener {

    private final NotifyOrderPlacedUseCase notifyOrderPlacedUseCase;

    public OrderPlacedListener(NotifyOrderPlacedUseCase notifyOrderPlacedUseCase) {
        this.notifyOrderPlacedUseCase = notifyOrderPlacedUseCase;
    }

    @KafkaListener(topics = "${app.kafka.topics.order-placed}")
    public void onOrderPlaced(String payload) {
        notifyOrderPlacedUseCase.notify(new OrderPlacedNotification(payload));
    }
}
