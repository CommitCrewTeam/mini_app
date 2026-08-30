package com.miniecommerce.order.adapter.outbound.persistence.mapper;

import com.miniecommerce.order.adapter.outbound.persistence.OrderEntity;
import com.miniecommerce.order.adapter.outbound.persistence.OrderItemEntity;
import com.miniecommerce.order.domain.MoneyValue;
import com.miniecommerce.order.domain.OrderAggregateRoot;
import com.miniecommerce.order.domain.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderPersistenceMapper {

    public OrderEntity toEntity(OrderAggregateRoot order) {
        OrderEntity entity = new OrderEntity();
        entity.setId(order.getId());
        entity.setCustomerId(order.getCustomerId());
        entity.setStatus(order.getStatus());
        entity.setShippingFee(order.getShippingFee().getAmount());
        entity.setTotalAmount(order.totalAmount().getAmount());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt());
        order.getItems().forEach(item -> {
            OrderItemEntity itemEntity = new OrderItemEntity();
            itemEntity.setOrder(entity);
            itemEntity.setProductId(item.getProductId());
            itemEntity.setQuantity(item.getQuantity());
            itemEntity.setUnitPrice(item.getUnitPrice().getAmount());
            entity.getItems().add(itemEntity);
        });
        return entity;
    }

    public OrderAggregateRoot toDomain(OrderEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
                .map(item -> OrderItem.rehydrate(
                        item.getProductId(), item.getQuantity(), MoneyValue.of(item.getUnitPrice())))
                .toList();
        return OrderAggregateRoot.rehydrate(
                entity.getId(),
                entity.getCustomerId(),
                MoneyValue.of(entity.getShippingFee()),
                entity.getStatus(),
                items,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}