package com.miniecommerce.product.app.port.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record ProductCacheResponse(
        Long id,
        String name,
        BigDecimal price,
        String description,
        Integer stock,
        String cachedAt
) implements Serializable {
}
