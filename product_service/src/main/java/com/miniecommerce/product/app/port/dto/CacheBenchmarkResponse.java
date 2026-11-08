package com.miniecommerce.product.app.port.dto;

import java.io.Serializable;

public record CacheBenchmarkResponse(
        ProductCacheResponse product,
        String source,
        long executionTimeMs,
        String databaseLoadReduction,
        String explanation
) implements Serializable {
}
