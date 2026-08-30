package com.miniecommerce.order.domain;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class PlaceOrderDomainService {

    public OrderAggregateRoot placeOrder(OrderAggregateRoot order) {
        if (!order.hasItems()) {
            throw new AppException(
                    ErrorCode.BAD_REQUEST,
                    "Order must have at least one item to be placed");
        }
        return order;
    }
}