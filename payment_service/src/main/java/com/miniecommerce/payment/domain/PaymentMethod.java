package com.miniecommerce.payment.domain;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;

public class PaymentMethod {

    private Long id;
    private String code;
    private String name;
    private boolean active;

    public PaymentMethod() {
    }

    public PaymentMethod(Long id, String code, String name, boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
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
        if (!violations.isEmpty()) {
            throw new AppException(
                    ErrorCode.BAD_REQUEST,
                    "Invalid PaymentMethod: " + String.join("; ", violations));
        }
    }
}
