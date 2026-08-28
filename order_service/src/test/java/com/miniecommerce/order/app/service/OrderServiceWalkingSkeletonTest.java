package com.miniecommerce.order.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.order.app.port.inbound.CreateOrderUseCase;
import com.miniecommerce.order.app.port.outbound.InventoryPort;
import com.miniecommerce.order.app.port.outbound.SaveOrderPort;
import com.miniecommerce.order.domain.Order;
import com.miniecommerce.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;

class OrderServiceWalkingSkeletonTest {

    @Test
    void placesOrderWhenStockAvailable() {
        InventoryPort inventory = phoneId -> 10;
        SaveOrderPort save = order -> {
            order.setId(1L);
            return order;
        };
        CreateOrderUseCase useCase = new OrderService(inventory, save);

        Order order = new Order();
        order.setPhoneId(1L);
        order.setQuantity(2);

        Order saved = useCase.placeOrder(order);

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void throwsWhenStockNotEnough() {
        InventoryPort inventory = phoneId -> 1;
        SaveOrderPort save = order -> order;
        CreateOrderUseCase useCase = new OrderService(inventory, save);

        Order order = new Order();
        order.setPhoneId(1L);
        order.setQuantity(5);

        assertThatThrownBy(() -> useCase.placeOrder(order))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("code", "STOCK_NOT_ENOUGH");
    }
}
