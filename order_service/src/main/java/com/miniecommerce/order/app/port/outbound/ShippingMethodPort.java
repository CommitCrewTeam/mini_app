package com.miniecommerce.order.app.port.outbound;

import com.miniecommerce.order.domain.ShippingOption;

import java.util.List;

public interface ShippingMethodPort {

    List<ShippingOption> findActive();
}