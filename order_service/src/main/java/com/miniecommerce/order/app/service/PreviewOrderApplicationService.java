package com.miniecommerce.order.app.service;

import com.miniecommerce.order.app.command.PreviewOrderCommand;
import com.miniecommerce.order.app.port.inbound.PreviewOrderUseCase;
import com.miniecommerce.order.app.port.outbound.InventoryPort;
import com.miniecommerce.order.app.port.outbound.PaymentMethodPort;
import com.miniecommerce.order.app.port.outbound.ShippingMethodPort;
import com.miniecommerce.order.domain.InventoryItem;
import com.miniecommerce.order.domain.OrderPreview;
import com.miniecommerce.order.domain.PaymentOption;
import com.miniecommerce.order.domain.PreviewItem;
import com.miniecommerce.order.domain.ShippingOption;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class PreviewOrderApplicationService implements PreviewOrderUseCase {

    private final InventoryPort inventoryPort;
    private final ShippingMethodPort shippingMethodPort;
    private final PaymentMethodPort paymentMethodPort;

    public PreviewOrderApplicationService(InventoryPort inventoryPort,
                                          ShippingMethodPort shippingMethodPort,
                                          PaymentMethodPort paymentMethodPort) {
        this.inventoryPort = inventoryPort;
        this.shippingMethodPort = shippingMethodPort;
        this.paymentMethodPort = paymentMethodPort;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderPreview preview(PreviewOrderCommand command) {
        List<String> productIds = command.items().stream()
                .map(PreviewOrderCommand.Item::productId)
                .toList();

        CompletableFuture<List<InventoryItem>> inventoryFuture =
                CompletableFuture.supplyAsync(() -> inventoryPort.getItemsForOrderPreview(productIds));

        CompletableFuture<List<PreviewItem>> itemsFuture =
                inventoryFuture.thenApply(items -> buildItems(command.items(), items));

        CompletableFuture<List<ShippingOption>> shippingFuture =
                CompletableFuture.supplyAsync(shippingMethodPort::findActive);
        CompletableFuture<List<PaymentOption>> paymentFuture =
                CompletableFuture.supplyAsync(paymentMethodPort::findActive);

        CompletableFuture.allOf(itemsFuture, shippingFuture, paymentFuture).join();

        List<PreviewItem> items = itemsFuture.join();
        long subtotal = computeSubtotal(items);
        long totalAmount = subtotal;

        return new OrderPreview(
                command.customerId(),
                items,
                shippingFuture.join(),
                paymentFuture.join(),
                subtotal,
                totalAmount);
    }

    private List<PreviewItem> buildItems(List<PreviewOrderCommand.Item> requestedItems, List<InventoryItem> inventoryItems) {
        List<PreviewItem> items = new ArrayList<>(requestedItems.size());
        for (int i = 0; i < requestedItems.size(); i++) {
            PreviewOrderCommand.Item requested = requestedItems.get(i);
            InventoryItem inventory = inventoryItems.get(i);
            items.add(new PreviewItem(
                    requested.productId(),
                    requested.quantity(),
                    requested.unitPrice(),
                    inventory.name(),
                    inventory.detail(),
                    inventory.active(),
                    inventory.stock(),
                    inventory.isAvailable(requested.quantity())));
        }
        return items;
    }

    private long computeSubtotal(List<PreviewItem> items) {
        long subtotal = 0L;
        for (PreviewItem item : items) {
            subtotal += item.unitPrice() * item.quantity();
        }
        return subtotal;
    }
}