package com.miniecommerce.order.adapter.inbound.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.miniecommerce.order.app.port.inbound.CreateOrderUseCase;
import org.junit.jupiter.api.Test;

class OrderControllerTest {

    @Test
    void step3_placesOrderThroughRestDrivingAdapter() {
        CreateOrderUseCase useCase = new FakeCreateOrderUseCase();
        OrderController controller = new OrderController(useCase);

        String result = controller.placeOrder();

        assertThat(result).isEqualTo("ORDER_CREATED_THROUGH_FAKE_USE_CASE");
    }

    private static class FakeCreateOrderUseCase implements CreateOrderUseCase {

        @Override
        public String placeOrder() {
            return "ORDER_CREATED_THROUGH_FAKE_USE_CASE";
        }
    }
}
