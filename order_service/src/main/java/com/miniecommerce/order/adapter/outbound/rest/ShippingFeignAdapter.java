package com.miniecommerce.order.adapter.outbound.rest;

import com.miniecommerce.order.app.port.outbound.ShippingMethodPort;
import com.miniecommerce.order.domain.ShippingOption;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ShippingFeignAdapter implements ShippingMethodPort {

    private final ShippingClient shippingClient;

    public ShippingFeignAdapter(ShippingClient shippingClient) {
        this.shippingClient = shippingClient;
    }

    @Override
    public List<ShippingOption> findActive() {
        var response = shippingClient.listActive();
        if (response == null || response.getData() == null) {
            return Collections.emptyList();
        }
        return response.getData().stream()
                .filter(dto -> Boolean.TRUE.equals(dto.active()))
                .map(dto -> new ShippingOption(
                        dto.code(), dto.name(), dto.baseFee() == null ? 0L : dto.baseFee().longValue()))
                .toList();
    }
}