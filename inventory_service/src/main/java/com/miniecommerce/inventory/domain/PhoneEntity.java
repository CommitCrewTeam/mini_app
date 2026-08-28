package com.miniecommerce.inventory.domain;

import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("phones")
public class PhoneEntity {

    @Id
    private Long id;
    private String name;
    private Json detail;
    private boolean active;
    private int stock;

    public PhoneEntity() {
    }

    public PhoneEntity(Long id, String name, Json detail, boolean active, int stock) {
        this.id = id;
        this.name = name;
        this.detail = detail;
        this.active = active;
        this.stock = stock;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
