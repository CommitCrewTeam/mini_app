package com.miniecommerce.inventory.domain;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Phone {

    private Long id;
    private String name;
    private Map<String, Object> detail;
    private boolean active;
    private int stock;

    public Phone() {
    }

    public Phone(Long id, String name, Map<String, Object> detail, boolean active, int stock) {
        this.id = id;
        this.name = name;
        this.detail = detail;
        this.active = active;
        this.stock = stock;
    }

    /** Imported phones start inactive until explicitly activated. */
    public static Phone fromImport(String key, Map<String, Object> detail, int quantity) {
        Phone phone = new Phone(null, key, detail, false, quantity);
        phone.validate();
        return phone;
    }

    /** Replace only the imported details. */
    public void updateFromImport(Map<String, Object> detail) {
        this.detail = detail;
    }

    /** Add stock without changing the phone's other information. */
    public void restock(int quantity) {
        requireNonNegativeQuantity(quantity);
        this.stock = Math.addExact(this.stock, quantity);
    }

    private static void requireNonNegativeQuantity(int quantity) {
        if (quantity < 0) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Stock quantity must not be negative");
        }
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

    public Map<String, Object> getDetail() {
        return detail;
    }

    public void setDetail(Map<String, Object> detail) {
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

    public boolean isAvailable() {
        return active && stock > 0;
    }

    public void validate() {
        List<String> violations = new ArrayList<>();
        if (name == null || name.isBlank()) {
            violations.add("name must not be blank");
        }
        if (stock < 0) {
            violations.add("stock must not be negative");
        }
        if (!violations.isEmpty()) {
            throw new AppException(
                    ErrorCode.BAD_REQUEST,
                    "Invalid Phone: " + String.join("; ", violations));
        }
    }
}
