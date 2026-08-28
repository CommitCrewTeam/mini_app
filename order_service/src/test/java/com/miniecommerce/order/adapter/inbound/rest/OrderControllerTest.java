package com.miniecommerce.order.adapter.inbound.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.miniecommerce.order.adapter.inbound.rest.dto.OrderRequest;
import com.miniecommerce.order.adapter.inbound.rest.mapper.OrderRestMapper;
import com.miniecommerce.order.app.port.inbound.CreateOrderUseCase;
import com.miniecommerce.order.domain.Order;
import com.miniecommerce.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;

class OrderControllerTest {

    @Test
    void placesOrderThroughRestDrivingAdapter() {
        CreateOrderUseCase useCase = order -> {
            order.setId(1L);
            order.setStatus(OrderStatus.PENDING);
            return order;
        };
        OrderRestMapper mapper = request -> {
            Order o = new Order();
            o.setPhoneId(request.phoneId());
            o.setQuantity(request.quantity());
            return o;
        };
        OrderController controller = new OrderController(useCase, mapper);

        OrderRequest request = new OrderRequest(1L, 2);
        Order saved = controller.placeOrder(request).getData();

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
    }
}
