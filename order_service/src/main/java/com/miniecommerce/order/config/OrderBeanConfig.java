package com.miniecommerce.order.config;

import com.miniecommerce.order.app.port.inbound.CreateOrderUseCase;
import com.miniecommerce.order.app.port.outbound.SaveOrderPort;
import com.miniecommerce.order.app.service.OrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderBeanConfig {

    @Bean
    CreateOrderUseCase createOrderUseCase(SaveOrderPort saveOrderPort) {
        return new OrderService(saveOrderPort);
    }
}
