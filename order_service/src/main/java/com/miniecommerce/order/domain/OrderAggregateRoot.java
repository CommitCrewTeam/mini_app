package com.miniecommerce.order.domain;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class OrderAggregateRoot {

    private final String id;
    private final String customerId;
    private final List<OrderItem> items;
    private final MoneyValue shippingFee;
    private final Instant createdAt;
    private OrderStatus status;
    private Instant updatedAt;
    private Instant confirmationDeadline;
    private String cancellationReason;
    private Long version;

    private OrderAggregateRoot(String id, String customerId, MoneyValue shippingFee, OrderStatus status,
                               List<OrderItem> items, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.shippingFee = shippingFee;
        this.status = status;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.confirmationDeadline = createdAt.plus(Duration.ofMinutes(30));
    }

    public static OrderAggregateRoot create(String customerId, MoneyValue shippingFee) {
        return create(customerId, shippingFee, Instant.now(), Duration.ofMinutes(30));
    }

    public static OrderAggregateRoot create(String customerId, MoneyValue shippingFee,
                                             Instant now, Duration confirmationTimeout) {
        if (confirmationTimeout.isZero() || confirmationTimeout.isNegative()) {
            throw new IllegalArgumentException("confirmationTimeout must be positive");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "customerId must not be blank");
        }
        if (shippingFee == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "shippingFee must not be null");
        }
        OrderAggregateRoot order = new OrderAggregateRoot(UUID.randomUUID().toString(), customerId, shippingFee,
                OrderStatus.PENDING, new ArrayList<>(), now, now);
        order.confirmationDeadline = now.plus(confirmationTimeout);
        return order;
    }

    public static OrderAggregateRoot rehydrate(String id, String customerId, MoneyValue shippingFee,
                                               OrderStatus status, List<OrderItem> items,
                                               Instant createdAt, Instant updatedAt) {
        return new OrderAggregateRoot(id, customerId, shippingFee, status, new ArrayList<>(items), createdAt, updatedAt);
    }

    public void addItem(String productId, int quantity, MoneyValue unitPrice) {
        assertModifiable();
        items.add(new OrderItem(productId, quantity, unitPrice));
        touch();
    }

    public static OrderAggregateRoot rehydrate(String id, String customerId, MoneyValue shippingFee,
                                               OrderStatus status, List<OrderItem> items,
                                               Instant createdAt, Instant updatedAt, Instant confirmationDeadline,
                                               String cancellationReason, Long version) {
        OrderAggregateRoot order = rehydrate(id, customerId, shippingFee, status, items, createdAt, updatedAt);
        order.confirmationDeadline = confirmationDeadline;
        order.cancellationReason = cancellationReason;
        order.version = version;
        return order;
    }

    public boolean expire(Instant now) {
        if (status != OrderStatus.PENDING || now.isBefore(confirmationDeadline)) {
            return false;
        }
        status = OrderStatus.CANCELLED;
        cancellationReason = "CONFIRMATION_TIMEOUT";
        updatedAt = now;
        return true;
    }

    public Instant getConfirmationDeadline() { return confirmationDeadline; }
    public String getCancellationReason() { return cancellationReason; }
    public Long getVersion() { return version; }

    public void removeItem(String productId) {
        assertModifiable();
        boolean removed = items.removeIf(item -> item.getProductId().equals(productId));
        if (!removed) {
            throw new AppException(ErrorCode.BAD_REQUEST, "No item with productId " + productId);
        }
        touch();
    }

    public void updateItemQuantity(String productId, int newQuantity) {
        assertModifiable();
        if (newQuantity <= 0) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "quantity must be > 0, but was " + newQuantity);
        }
        int index = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getProductId().equals(productId)) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            throw new AppException(ErrorCode.BAD_REQUEST, "No item with productId " + productId);
        }
        items.set(index, new OrderItem(productId, newQuantity, items.get(index).getUnitPrice()));
        touch();
    }

    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.CONFLICT,
                    "Order can only be confirmed from PENDING, current=" + status);
        }
        if (items.isEmpty()) {
            throw new AppException(ErrorCode.CONFLICT,
                    "Order must have at least one item to be confirmed");
        }
        status = OrderStatus.CONFIRMED;
        touch();
    }

    public void cancel() {
        if (status == OrderStatus.CANCELLED) {
            throw new AppException(ErrorCode.CONFLICT, "Order is already CANCELLED");
        }
        status = OrderStatus.CANCELLED;
        touch();
    }

    public MoneyValue totalAmount() {
        MoneyValue subtotal = items.stream()
                .map(OrderItem::subtotal)
                .reduce(MoneyValue.ZERO, MoneyValue::add);
        return subtotal.add(shippingFee);
    }

    public MoneyValue getTotalAmount() {
        return totalAmount();
    }

    public boolean hasItems() {
        return !items.isEmpty();
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public MoneyValue getShippingFee() {
        return shippingFee;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void assertModifiable() {
        if (status != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.CONFLICT,
                    "Order items can only be modified when status is PENDING, current=" + status);
        }
    }

    private void touch() {
        updatedAt = Instant.now();
    }
}
