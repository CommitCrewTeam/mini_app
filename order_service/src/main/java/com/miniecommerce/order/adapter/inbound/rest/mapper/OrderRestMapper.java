package com.miniecommerce.order.adapter.inbound.rest.mapper;

import com.miniecommerce.order.adapter.inbound.rest.dto.OrderRequest;
import com.miniecommerce.order.app.command.CreateOrderCommand;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderRestMapper {

    public CreateOrderCommand toCommand(OrderRequest request) {
        List<CreateOrderCommand.Item> items = request.items().stream()
                .map(item -> new CreateOrderCommand.Item(item.productId(), item.quantity(), item.unitPrice()))
                .toList();
        return new CreateOrderCommand(request.customerId(), request.shippingFee(), items);
    }
}