package com.miniecommerce.order.adapter.inbound.rest.dto;

import java.util.List;
import java.util.Map;

public record OrderPreviewResponse(
        String customerId,
        List<PreviewItemResponse> items,
        List<ShippingOptionResponse> shippingOptions,
        List<PaymentOptionResponse> paymentOptions,
        long subtotal,
        long totalAmount) {

    public record PreviewItemResponse(
            String productId,
            int quantity,
            long unitPrice,
            String name,
            Map<String, Object> detail,
            boolean active,
            int stock,
            boolean available) {
    }

    public record ShippingOptionResponse(String code, String name, long baseFee) {
    }

    public record PaymentOptionResponse(String code, String name) {
    }
}