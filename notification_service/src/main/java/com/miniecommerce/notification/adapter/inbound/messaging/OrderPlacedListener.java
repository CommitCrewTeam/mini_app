package com.miniecommerce.notification.adapter.inbound.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.notification.app.port.inbound.NotifyOrderPlacedUseCase;
import com.miniecommerce.notification.domain.event.OrderPlacedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderPlacedListener {

    private final NotifyOrderPlacedUseCase notifyOrderPlacedUseCase;
    private final ObjectMapper objectMapper;

    public OrderPlacedListener(NotifyOrderPlacedUseCase notifyOrderPlacedUseCase,
                               ObjectMapper objectMapper) {
        this.notifyOrderPlacedUseCase = notifyOrderPlacedUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.order-placed}")
    public void onOrderPlaced(String payload) {
        notifyOrderPlacedUseCase.notifyOrderPlaced(toEvent(payload));
    }

    private OrderPlacedEvent toEvent(String payload) {
        try {
            return objectMapper.readValue(payload, OrderPlacedEvent.class);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Invalid order.placed payload: " + e.getOriginalMessage());
        }
    }
}
