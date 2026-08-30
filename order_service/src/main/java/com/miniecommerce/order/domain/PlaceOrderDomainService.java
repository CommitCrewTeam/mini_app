package com.miniecommerce.order.domain;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class PlaceOrderDomainService {

    public boolean crossDomainValidate(OrderAggregateRoot order) {

        return true;
    }
}