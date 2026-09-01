package com.miniecommerce.order.adapter.outbound.rest;

import com.miniecommerce.order.app.port.outbound.PaymentMethodPort;
import com.miniecommerce.order.domain.PaymentOption;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class PaymentFeignAdapter implements PaymentMethodPort {

    private final PaymentClient paymentClient;

    public PaymentFeignAdapter(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
    }

    @Override
    public List<PaymentOption> findActive() {
        var response = paymentClient.listActive();
        if (response == null || response.getData() == null) {
            return Collections.emptyList();
        }
        return response.getData().stream()
                .filter(dto -> Boolean.TRUE.equals(dto.active()))
                .map(dto -> new PaymentOption(dto.code(), dto.name()))
                .toList();
    }
}