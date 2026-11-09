package com.miniecommerce.product.adapter.inbound.rest;

import com.miniecommerce.common.response.ApiResponse;
import com.miniecommerce.product.app.port.dto.ProductResponse;
import com.miniecommerce.product.app.service.ProductLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/product-lock")
@RequiredArgsConstructor
public class ProductLockController {
    private final ProductLockService flashSaleService;

    // Reset kho hàng
    @PostMapping("/reset/{productId}")
    public ApiResponse<String> resetStock(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "10") int stock
    ) {
        flashSaleService.resetStock(productId, stock);
        return ApiResponse.success("Đã reset kho cho sản phẩm ID " + productId + " về " + stock + " sản phẩm.");
    }

    // Lấy số lượng tồn kho hiện tại
    @GetMapping("/stock/{productId}")
    public ApiResponse<Integer> getStock(@PathVariable Long productId) {
        return ApiResponse.success(flashSaleService.getStock(productId));
    }

    // Mua 1 đơn lẻ không lock
    @PostMapping("/buy-unsafe/{productId}")
    public ApiResponse<String> buyUnsafe(@PathVariable Long productId) {
        boolean success = flashSaleService.buyWithoutLock(productId);
        if (success) {
            return ApiResponse.success("Đặt hàng thành công (Unsafe)! Tồn kho còn: " + flashSaleService.getStock(productId));
        } else {
            return ApiResponse.error("400", "Đặt hàng thất bại: Hết hàng!");
        }
    }

    // Mua 1 đơn lẻ có Redisson Lock
    @PostMapping("/buy-safe/{productId}")
    public ApiResponse<String> buySafe(@PathVariable Long productId) {
        boolean success = flashSaleService.buyWithRedissonLock(productId);
        if (success) {
            return ApiResponse.success("Đặt hàng thành công (Redisson Lock)! Tồn kho còn: " + flashSaleService.getStock(productId));
        } else {
            return ApiResponse.error("400", "Đặt hàng thất bại: Hết hàng!");
        }
    }

    // Giả lập 100 User bấm Mua cùng lúc KHÔNG DÙNG LOCK (Hiện tượng Race Condition / Overselling)
    @PostMapping("/simulate-race-condition/{productId}")
    public ApiResponse<ProductResponse> simulateRaceCondition(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "100") int concurrentRequests
    ) {
        ProductResponse response = flashSaleService.runConcurrentTest(productId, false, concurrentRequests);
        return ApiResponse.success(response);
    }

    // Giả lập 100 User bấm Mua cùng lúc CÓ REDISSON LOCK (Safe 100%)
    @PostMapping("/simulate-distributed-lock/{productId}")
    public ApiResponse<ProductResponse> simulateDistributedLock(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "100") int concurrentRequests
    ) {
        ProductResponse response = flashSaleService.runConcurrentTest(productId, true, concurrentRequests);
        return ApiResponse.success(response);
    }
}
