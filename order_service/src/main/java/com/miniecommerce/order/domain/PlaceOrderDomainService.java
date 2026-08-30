package com.miniecommerce.order.domain;

import com.miniecommerce.common.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PlaceOrderDomainService {

    public OrderAggregateRoot placeOrder(OrderAggregateRoot order) {
        if (!order.hasItems()) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "ORDER_HAS_NO_ITEMS",
                    "Order must have at least one item to be placed");
        }
        return order;
    }
}