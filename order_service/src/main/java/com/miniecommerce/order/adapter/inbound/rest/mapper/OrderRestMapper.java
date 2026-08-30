package com.miniecommerce.order.adapter.inbound.rest.mapper;

import com.miniecommerce.order.adapter.inbound.rest.dto.OrderRequest;
import com.miniecommerce.order.adapter.inbound.rest.dto.OrderResponse;
import com.miniecommerce.order.app.command.CreateOrderCommand;
import com.miniecommerce.order.domain.OrderAggregateRoot;
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

    public OrderResponse toResponse(OrderAggregateRoot order) {
        List<OrderResponse.ItemResponse> items = order.getItems().stream()
                .map(item -> new OrderResponse.ItemResponse(
                        item.getProductId(), item.getQuantity(), item.getUnitPrice().getAmount()))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                items,
                order.getShippingFee().getAmount(),
                order.totalAmount().getAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}