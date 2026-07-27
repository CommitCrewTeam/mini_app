package com.miniecommerce.notification.application;

import com.miniecommerce.notification.domain.OrderPlacedNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotifyOrderPlacedUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotifyOrderPlacedUseCase.class);

    public void notify(OrderPlacedNotification notification) {
        log.info("Received order placed event: {}", notification.payload());
    }
}
