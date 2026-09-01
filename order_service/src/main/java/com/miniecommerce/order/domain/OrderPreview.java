package com.miniecommerce.order.domain;

import java.util.List;

public record OrderPreview(
        String customerId,
        List<PreviewItem> items,
        List<ShippingOption> shippingOptions,
        List<PaymentOption> paymentOptions,
        long subtotal,
        long totalAmount) {
}