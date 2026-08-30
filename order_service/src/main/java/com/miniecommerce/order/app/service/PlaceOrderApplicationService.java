package com.miniecommerce.order.app.service;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.order.app.command.CreateOrderCommand;
import com.miniecommerce.order.app.port.inbound.CreateOrderUseCase;
import com.miniecommerce.order.app.port.outbound.PublishOrderEventPort;
import com.miniecommerce.order.app.port.outbound.SaveOrderPort;
import com.miniecommerce.order.domain.MoneyValue;
import com.miniecommerce.order.domain.OrderAggregateRoot;
import com.miniecommerce.order.domain.PlaceOrderDomainService;
import com.miniecommerce.order.domain.event.OrderPlacedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PlaceOrderApplicationService implements CreateOrderUseCase {

    private final SaveOrderPort saveOrderPort;
    private final PublishOrderEventPort publishOrderEventPort;
    private final PlaceOrderDomainService placeOrderDomainService;

    public PlaceOrderApplicationService(SaveOrderPort saveOrderPort,
                                        PublishOrderEventPort publishOrderEventPort,
                                        PlaceOrderDomainService placeOrderDomainService) {
        this.saveOrderPort = saveOrderPort;
        this.publishOrderEventPort = publishOrderEventPort;
        this.placeOrderDomainService = placeOrderDomainService;
    }

    @Override
    @Transactional
    public OrderAggregateRoot placeOrder(CreateOrderCommand command) {
        OrderAggregateRoot order = OrderAggregateRoot.create(
                command.customerId(), MoneyValue.of(command.shippingFee()));
        command.items().forEach(item ->
                order.addItem(item.productId(), item.quantity(), MoneyValue.of(item.unitPrice())));

         if (!placeOrderDomainService.crossDomainValidate(order)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Order failed cross-domain validation");
        }
        OrderAggregateRoot saved = saveOrderPort.save(order);
        publishOrderEventPort.publishOrderCreated(toOrderPlacedEvent(saved));
        return saved;
    }

    private OrderPlacedEvent toOrderPlacedEvent(OrderAggregateRoot order) {
        List<OrderPlacedEvent.Item> items = order.getItems().stream()
                .map(item -> new OrderPlacedEvent.Item(
                        item.getProductId(), item.getQuantity(), item.getUnitPrice().getAmount()))
                .toList();
        return new OrderPlacedEvent(
                UUID.randomUUID().toString(),
                order.getId(),
                order.getCustomerId(),
                items,
                order.getShippingFee().getAmount(),
                order.totalAmount().getAmount(),
                order.getStatus(),
                order.getCreatedAt());
    }
}