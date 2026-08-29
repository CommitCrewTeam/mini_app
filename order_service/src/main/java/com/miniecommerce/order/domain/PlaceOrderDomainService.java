package com.miniecommerce.order.domain;

import com.miniecommerce.common.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PlaceOrderDomainService {

    public Order placeOrder(Order order, int availableStock) {
        if (availableStock < order.getQuantity()) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    "STOCK_NOT_ENOUGH",
                    "Not enough stock for phone " + order.getPhoneId());
        }
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now());
        return order;
    }
}
