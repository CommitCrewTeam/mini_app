package com.miniecommerce.order.domain;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;

import java.util.Objects;

public final class OrderItem {

    private final String productId;
    private final int quantity;
    private final MoneyValue unitPrice;

    OrderItem(String productId, int quantity, MoneyValue unitPrice) {
        if (productId == null || productId.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "productId must not be blank");
        }
        if (quantity <= 0) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "quantity must be > 0, but was " + quantity);
        }
        if (unitPrice == null || unitPrice.getAmount() < 0) {
            throw new AppException(ErrorCode.BAD_REQUEST, "unitPrice must be >= 0");
        }
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public static OrderItem rehydrate(String productId, int quantity, MoneyValue unitPrice) {
        return new OrderItem(productId, quantity, unitPrice);
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public MoneyValue getUnitPrice() {
        return unitPrice;
    }

    public MoneyValue subtotal() {
        return unitPrice.multiply(quantity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderItem orderItem)) {
            return false;
        }
        return quantity == orderItem.quantity
                && productId.equals(orderItem.productId)
                && unitPrice.equals(orderItem.unitPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, quantity, unitPrice);
    }

    @Override
    public String toString() {
        return "OrderItem{" + "productId='" + productId + '\''
                + ", quantity=" + quantity
                + ", unitPrice=" + unitPrice + '}';
    }
}