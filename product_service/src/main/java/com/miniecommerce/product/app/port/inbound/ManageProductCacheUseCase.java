package com.miniecommerce.product.app.port.inbound;

import com.miniecommerce.product.app.port.dto.CacheBenchmarkResponse;
import com.miniecommerce.product.app.port.dto.ProductCacheResponse;

public interface ManageProductCacheUseCase {
    ProductCacheResponse getProductById(Long id);
    CacheBenchmarkResponse getProductWithBenchmark(Long id);
    ProductCacheResponse updateProduct(Long id, ProductCacheResponse request);
    void deleteProduct(Long id);
    void clearAllCache();
}
