package com.miniecommerce.product.domain;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Product {

    private Long id;
    private Long categoryId;
    private String name;
    private String sku;
    private String description;
    private BigDecimal price;
    private ProductStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public Product() {
    }

    public Product(Long id, Long categoryId, String name, String sku, String description,
                   BigDecimal price, ProductStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.sku = sku;
        this.description = description;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isSellable() {
        return status == ProductStatus.ACTIVE;
    }

    public void validate() {
        List<String> violations = new ArrayList<>();
        if (name == null || name.isBlank()) {
            violations.add("name must not be blank");
        }
        if (sku == null || sku.isBlank()) {
            violations.add("sku must not be blank");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            violations.add("price must not be null or negative");
        }
        if (status == null) {
            violations.add("status must not be null");
        }
        if (!violations.isEmpty()) {
            throw new AppException(
                    ErrorCode.BAD_REQUEST,
                    "Invalid Product: " + String.join("; ", violations));
        }
    }
}
