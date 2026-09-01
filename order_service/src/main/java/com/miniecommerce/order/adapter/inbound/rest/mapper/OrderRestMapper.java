package com.miniecommerce.order.adapter.inbound.rest.mapper;

import com.miniecommerce.order.adapter.inbound.rest.dto.OrderPreviewResponse;
import com.miniecommerce.order.adapter.inbound.rest.dto.OrderRequest;
import com.miniecommerce.order.adapter.inbound.rest.dto.OrderResponse;
import com.miniecommerce.order.app.command.CreateOrderCommand;
import com.miniecommerce.order.app.command.PreviewOrderCommand;
import com.miniecommerce.order.domain.OrderAggregateRoot;
import com.miniecommerce.order.domain.OrderPreview;
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

    public PreviewOrderCommand toPreviewCommand(OrderRequest request) {
        List<PreviewOrderCommand.Item> items = request.items().stream()
                .map(item -> new PreviewOrderCommand.Item(item.productId(), item.quantity(), item.unitPrice()))
                .toList();
        return new PreviewOrderCommand(request.customerId(), items);
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

    public OrderPreviewResponse toPreviewResponse(OrderPreview preview) {
        List<OrderPreviewResponse.PreviewItemResponse> items = preview.items().stream()
                .map(item -> new OrderPreviewResponse.PreviewItemResponse(
                        item.productId(), item.quantity(), item.unitPrice(),
                        item.name(), item.detail(), item.active(), item.stock(), item.available()))
                .toList();
        List<OrderPreviewResponse.ShippingOptionResponse> shippingOptions = preview.shippingOptions().stream()
                .map(option -> new OrderPreviewResponse.ShippingOptionResponse(
                        option.code(), option.name(), option.baseFee()))
                .toList();
        List<OrderPreviewResponse.PaymentOptionResponse> paymentOptions = preview.paymentOptions().stream()
                .map(option -> new OrderPreviewResponse.PaymentOptionResponse(option.code(), option.name()))
                .toList();
        return new OrderPreviewResponse(
                preview.customerId(),
                items,
                shippingOptions,
                paymentOptions,
                preview.subtotal(),
                preview.totalAmount());
    }
}