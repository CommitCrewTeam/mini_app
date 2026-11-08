package com.miniecommerce.product.adapter.outbound.persistence;

import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("product_variants")
public class ProductVariantEntity {

    @Id
    private Long id;
    private Long productId;
    private String sku;
    private Json attributesJson;
    private BigDecimal priceAdjustment;

    public ProductVariantEntity() {
    }

    public ProductVariantEntity(Long id, Long productId, String sku, Json attributesJson, BigDecimal priceAdjustment) {
        this.id = id;
        this.productId = productId;
        this.sku = sku;
        this.attributesJson = attributesJson;
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

    public Json getAttributesJson() {
        return attributesJson;
    }

    public void setAttributesJson(Json attributesJson) {
        this.attributesJson = attributesJson;
    }

    public BigDecimal getPriceAdjustment() {
        return priceAdjustment;
    }

    public void setPriceAdjustment(BigDecimal priceAdjustment) {
        this.priceAdjustment = priceAdjustment;
    }
}
