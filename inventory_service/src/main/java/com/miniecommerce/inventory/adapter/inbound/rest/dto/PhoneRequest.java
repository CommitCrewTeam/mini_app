package com.miniecommerce.inventory.adapter.inbound.rest.dto;

import java.util.Map;

public record PhoneRequest(String key, Map<String, Object> detail, boolean active, int stock) {
}
