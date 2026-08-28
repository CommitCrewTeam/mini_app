package com.miniecommerce.order.adapter.outbound.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.order.app.port.outbound.PublishOrderEventPort;
import com.miniecommerce.order.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
    public void publishOrderCreated(Order order) {
        try {
            String payload = objectMapper.writeValueAsString(order);
            kafkaTemplate.send(topic, String.valueOf(order.getId()), payload)
                    .whenComplete((result, failure) -> {
                        if (failure != null) {
                            log.error("Failed to publish OrderCreatedEvent for orderId={}", order.getId(), failure);
                        } else {
                            log.info("Published OrderCreatedEvent orderId={} to topic={}", order.getId(), topic);
                        }
                    });
        } catch (JsonProcessingException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "EVENT_SERIALIZATION_FAILED",
                    "Cannot serialize order event: " + e.getMessage());
        }
    }
}
