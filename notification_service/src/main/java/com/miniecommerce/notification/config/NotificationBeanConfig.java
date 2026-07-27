package com.miniecommerce.notification.config;

import com.miniecommerce.notification.app.port.inbound.NotifyOrderPlacedUseCase;
import com.miniecommerce.notification.app.port.outbound.SendOrderPlacedNotificationPort;
import com.miniecommerce.notification.app.service.NotificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationBeanConfig {

    @Bean
    NotifyOrderPlacedUseCase notifyOrderPlacedUseCase(
            SendOrderPlacedNotificationPort sendOrderPlacedNotificationPort
    ) {
        return new NotificationService(sendOrderPlacedNotificationPort);
    }
}
