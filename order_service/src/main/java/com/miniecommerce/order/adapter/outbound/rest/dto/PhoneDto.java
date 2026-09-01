package com.miniecommerce.order.adapter.outbound.rest.dto;

import java.util.Map;

public record PhoneDto(Long id, String name, Map<String, Object> detail, boolean active, int stock) {
}