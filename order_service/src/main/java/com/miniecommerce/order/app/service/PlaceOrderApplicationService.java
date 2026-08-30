package com.miniecommerce.order.app.service;

import com.miniecommerce.order.app.command.CreateOrderCommand;
import com.miniecommerce.order.app.port.inbound.CreateOrderUseCase;
import com.miniecommerce.order.app.port.outbound.PublishOrderEventPort;
import com.miniecommerce.order.app.port.outbound.SaveOrderPort;
import com.miniecommerce.order.domain.MoneyValue;
import com.miniecommerce.order.domain.OrderAggregateRoot;
import com.miniecommerce.order.domain.PlaceOrderDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        OrderAggregateRoot pending = placeOrderDomainService.placeOrder(order);
        OrderAggregateRoot saved = saveOrderPort.save(pending);
        publishOrderEventPort.publishOrderCreated(saved);
        return saved;
    }
}