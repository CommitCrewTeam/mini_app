package com.miniecommerce.shipping.app.port.outbound;

import com.miniecommerce.shipping.domain.ShippingMethod;

import java.util.List;

public interface ShippingMethodRepository {

    List<ShippingMethod> findActive();
}