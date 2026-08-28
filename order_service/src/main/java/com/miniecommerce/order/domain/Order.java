package com.miniecommerce.order.domain;

import java.time.Instant;

public class Order {

    private Long id;
    private Long phoneId;
    private int quantity;
    private OrderStatus status;
    private Instant createdAt;

    public Order() {
    }

    public Order(Long id, Long phoneId, int quantity, OrderStatus status, Instant createdAt) {
        this.id = id;
        this.phoneId = phoneId;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPhoneId() {
        return phoneId;
    }

    public void setPhoneId(Long phoneId) {
        this.phoneId = phoneId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
