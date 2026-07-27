package com.miniecommerce.notification.adapter.inbound.kafka;

import com.miniecommerce.notification.app.port.inbound.NotifyOrderPlacedUseCase;
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
        notifyOrderPlacedUseCase.notifyOrderPlaced();
    }
}
