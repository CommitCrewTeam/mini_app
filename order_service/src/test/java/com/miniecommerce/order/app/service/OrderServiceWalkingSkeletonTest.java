package com.miniecommerce.order.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.miniecommerce.order.app.port.in.CreateOrderUseCase;
import org.junit.jupiter.api.Test;

class OrderServiceWalkingSkeletonTest {

    @Test
    void step1_placesOrderThroughDrivingPortAndReturnsConstant() {
        CreateOrderUseCase useCase = new OrderService();

        String result = useCase.placeOrder();

        assertThat(result).isEqualTo("ORDER_CREATED");
    }
}
