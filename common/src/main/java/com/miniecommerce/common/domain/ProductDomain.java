package com.miniecommerce.common.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

public record ProductDomain(
        Long id,
        String sku,
        Long categoryId,
        String name,
        String description,
        BigDecimal price,
        Map<String, Object> attributes,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
