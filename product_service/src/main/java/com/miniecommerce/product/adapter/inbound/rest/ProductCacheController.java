package com.miniecommerce.product.adapter.inbound.rest;

import com.miniecommerce.common.controller.BaseController;
import com.miniecommerce.common.response.ApiResponse;
import com.miniecommerce.product.app.port.dto.CacheBenchmarkResponse;
import com.miniecommerce.product.app.port.dto.ProductCacheResponse;
import com.miniecommerce.product.app.port.inbound.ManageProductCacheUseCase;
import com.miniecommerce.product.app.service.ProductCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cache-demo/products")
@RequiredArgsConstructor
public class ProductCacheController extends BaseController {

    private final ManageProductCacheUseCase productCacheService;

    // Demo @Cacheable: Lấy thông tin sản phẩm (được lưu cache tự động)
    @GetMapping("/{id}")
    public ApiResponse<ProductCacheResponse> getProductById(@PathVariable Long id) {
        ProductCacheResponse product = productCacheService.getProductById(id);
        if (product == null) {
            return ApiResponse.error("404", "Không tìm thấy sản phẩm với ID: " + id);
        }
        return ApiResponse.success(product);
    }

    // Benchmark API: Hiển thị trực quan chỉ số hiệu năng (Cache MISS vs Cache HIT)
    @GetMapping("/{id}/benchmark")
    public ApiResponse<CacheBenchmarkResponse> getProductBenchmark(@PathVariable Long id) {
        CacheBenchmarkResponse benchmark = productCacheService.getProductWithBenchmark(id);
        if (benchmark.product() == null) {
            return ApiResponse.error("404", "Không tìm thấy sản phẩm với ID: " + id);
        }
        return ApiResponse.success(benchmark);
    }

    // Demo @CachePut: Cập nhật sản phẩm & đồng bộ ngay lập tức vào Cache
    @PutMapping("/{id}")
    public ApiResponse<ProductCacheResponse> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductCacheResponse request
    ) {
        ProductCacheResponse updatedProduct = productCacheService.updateProduct(id, request);
        return ApiResponse.success(updatedProduct);
    }

    // Demo @CacheEvict: Xóa sản phẩm & làm sạch key tương ứng trong Cache
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteProduct(@PathVariable Long id) {
        productCacheService.deleteProduct(id);
        return ApiResponse.success("Đã xóa sản phẩm ID: " + id + " và xóa Cache thành công!");
    }

    // Demo @CacheEvict(allEntries = true): Xóa toàn bộ Cache namespace 'product_detail'
    @DeleteMapping("/clear-all")
    public ApiResponse<String> clearAllCache() {
        productCacheService.clearAllCache();
        return ApiResponse.success("Đã làm sạch toàn bộ Cache 'product_detail' thành công!");
    }
}
