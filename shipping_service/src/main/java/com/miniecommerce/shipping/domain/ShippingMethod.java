package com.miniecommerce.shipping.domain;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ShippingMethod {

    private Long id;
    private String code;
    private String name;
    private BigDecimal baseFee;
    private boolean active;

    public ShippingMethod() {
    }

    public ShippingMethod(Long id, String code, String name, BigDecimal baseFee, boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.baseFee = baseFee;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getBaseFee() {
        return baseFee;
    }

    public void setBaseFee(BigDecimal baseFee) {
        this.baseFee = baseFee;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void validate() {
        List<String> violations = new ArrayList<>();
        if (code == null || code.isBlank()) {
            violations.add("code must not be blank");
        }
        if (name == null || name.isBlank()) {
            violations.add("name must not be blank");
        }
        if (baseFee == null || baseFee.signum() < 0) {
            violations.add("baseFee must not be negative");
        }
        if (!violations.isEmpty()) {
            throw new AppException(
                    ErrorCode.BAD_REQUEST,
                    "Invalid ShippingMethod: " + String.join("; ", violations));
        }
    }
}