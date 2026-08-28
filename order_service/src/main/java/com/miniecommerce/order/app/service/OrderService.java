package com.miniecommerce.order.app.service;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.order.app.port.inbound.CreateOrderUseCase;
import com.miniecommerce.order.app.port.outbound.InventoryPort;
import com.miniecommerce.order.app.port.outbound.SaveOrderPort;
import com.miniecommerce.order.domain.Order;
import com.miniecommerce.order.domain.OrderStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class OrderService implements CreateOrderUseCase {

    private final InventoryPort inventoryPort;
    private final SaveOrderPort saveOrderPort;

    public OrderService(InventoryPort inventoryPort, SaveOrderPort saveOrderPort) {
        this.inventoryPort = inventoryPort;
        this.saveOrderPort = saveOrderPort;
    }

    @Override
    public Order placeOrder(Order order) {
        int available = inventoryPort.getStock(order.getPhoneId());
        if (available < order.getQuantity()) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "STOCK_NOT_ENOUGH",
                    "Not enough stock for phone " + order.getPhoneId());
        }
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now());
        return saveOrderPort.save(order);
    }
}
