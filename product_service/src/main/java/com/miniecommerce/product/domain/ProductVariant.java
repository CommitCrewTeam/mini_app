package com.miniecommerce.product.domain;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProductVariant {

    private Long id;
    private Long productId;
    private String sku;
    private Map<String, Object> attributes;
    private BigDecimal priceAdjustment;

    public ProductVariant() {
    }

    public ProductVariant(Long id, Long productId, String sku, Map<String, Object> attributes,
                          BigDecimal priceAdjustment) {
        this.id = id;
        this.productId = productId;
        this.sku = sku;
        this.attributes = attributes;
        this.priceAdjustment = priceAdjustment;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public BigDecimal getPriceAdjustment() {
        return priceAdjustment;
    }

    public void setPriceAdjustment(BigDecimal priceAdjustment) {
        this.priceAdjustment = priceAdjustment;
    }

    public BigDecimal effectivePrice(BigDecimal basePrice) {
        return basePrice.add(priceAdjustment == null ? BigDecimal.ZERO : priceAdjustment);
    }

    public void validate() {
        List<String> violations = new ArrayList<>();
        if (productId == null) {
            violations.add("productId must not be null");
        }
        if (sku == null || sku.isBlank()) {
            violations.add("sku must not be blank");
        }
        if (priceAdjustment == null) {
            violations.add("priceAdjustment must not be null");
        }
        if (!violations.isEmpty()) {
            throw new AppException(
                    ErrorCode.BAD_REQUEST,
                    "Invalid ProductVariant: " + String.join("; ", violations));
        }
    }
}
