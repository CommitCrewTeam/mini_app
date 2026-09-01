package com.miniecommerce.order.adapter.outbound.rest.dto;

public record PaymentMethodDto(Long id, String code, String name, boolean active) {
}