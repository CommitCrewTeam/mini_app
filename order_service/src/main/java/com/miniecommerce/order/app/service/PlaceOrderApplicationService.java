package com.miniecommerce.order.app.service;

import com.miniecommerce.order.app.port.inbound.CreateOrderUseCase;
import com.miniecommerce.order.app.port.outbound.InventoryPort;
import com.miniecommerce.order.app.port.outbound.PublishOrderEventPort;
import com.miniecommerce.order.app.port.outbound.SaveOrderPort;
import com.miniecommerce.order.domain.Order;
import com.miniecommerce.order.domain.PlaceOrderDomainService;
import org.springframework.stereotype.Service;

@Service
public class PlaceOrderApplicationService implements CreateOrderUseCase {

    private final InventoryPort inventoryPort;
    private final SaveOrderPort saveOrderPort;
    private final PublishOrderEventPort publishOrderEventPort;
    private final PlaceOrderDomainService placeOrderDomainService;

    public PlaceOrderApplicationService(InventoryPort inventoryPort,
                                        SaveOrderPort saveOrderPort,
                                        PublishOrderEventPort publishOrderEventPort,
                                        PlaceOrderDomainService placeOrderDomainService) {
        this.inventoryPort = inventoryPort;
        this.saveOrderPort = saveOrderPort;
        this.publishOrderEventPort = publishOrderEventPort;
        this.placeOrderDomainService = placeOrderDomainService;
    }

    @Override
    public Order placeOrder(Order order) {
        int available = inventoryPort.getStock(order.getPhoneId());
        Order pending = placeOrderDomainService.placeOrder(order, available);
        Order saved = saveOrderPort.save(pending);
        publishOrderEventPort.publishOrderCreated(saved);
        return saved;
    }
}
