package com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc.entity;

import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

@Table("phone")
public class PhoneEntity {

    @Id
    private Long id;
    private String key;
    @Version
    private Long version;
    private Json detail;
    private boolean active;
    private int stock;

    public PhoneEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Json getDetail() {
        return detail;
    }

    public void setDetail(Json detail) {
        this.detail = detail;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
