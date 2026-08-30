package com.miniecommerce.order.adapter.inbound.rest.mapper;

import com.miniecommerce.order.adapter.inbound.rest.dto.OrderRequest;
import com.miniecommerce.order.domain.MoneyValue;
import com.miniecommerce.order.domain.OrderAggregateRoot;
import org.springframework.stereotype.Component;

@Component
public class OrderRestMapper {

    public OrderAggregateRoot toOrder(OrderRequest request) {
        OrderAggregateRoot order = OrderAggregateRoot.create(request.customerId(), MoneyValue.of(request.shippingFee()));
        request.items().forEach(item ->
                order.addItem(item.productId(), item.quantity(), MoneyValue.of(item.unitPrice())));
        return order;
    }
}