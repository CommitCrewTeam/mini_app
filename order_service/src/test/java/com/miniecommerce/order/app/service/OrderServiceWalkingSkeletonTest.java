package com.miniecommerce.order.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.miniecommerce.order.app.port.inbound.CreateOrderUseCase;
import com.miniecommerce.order.app.port.outbound.SaveOrderPort;
import org.junit.jupiter.api.Test;

class OrderServiceWalkingSkeletonTest {

    @Test
    void step2_placesOrderThroughDrivingPortAndUsesDrivenPort() {
        SaveOrderPort saveOrderPort = new FakeSaveOrderAdapter();
        CreateOrderUseCase useCase = new OrderService(saveOrderPort);

        String result = useCase.placeOrder();

        assertThat(result).isEqualTo("ORDER_SAVED_BY_FAKE_ADAPTER");
    }

    private static class FakeSaveOrderAdapter implements SaveOrderPort {

        @Override
        public String save() {
            return "ORDER_SAVED_BY_FAKE_ADAPTER";
        }
    }
}
