package com.miniecommerce.order.adapter.outbound.rest.dto;

import java.math.BigDecimal;

public record ShippingMethodDto(Long id, String code, String name, BigDecimal baseFee, boolean active) {
}