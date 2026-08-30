package com.miniecommerce.order.adapter.outbound.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.order.app.port.outbound.PublishOrderEventPort;
import com.miniecommerce.order.domain.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderKafkaProducer implements PublishOrderEventPort {

    private static final Logger log = LoggerFactory.getLogger(OrderKafkaProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public OrderKafkaProducer(KafkaTemplate<String, String> kafkaTemplate,
                              ObjectMapper objectMapper,
                              @Value("${app.kafka.topics.order-placed}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public void publishOrderCreated(OrderPlacedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, event.orderId(), payload)
                    .whenComplete((result, failure) -> {
                        if (failure != null) {
                            log.error("Failed to publish OrderPlacedEvent for orderId={}", event.orderId(), failure);
                        } else {
                            log.info("Published OrderPlacedEvent orderId={} to topic={}", event.orderId(), topic);
                        }
                    });
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR,
                    "Cannot serialize order event: " + e.getMessage());
        }
    }
}