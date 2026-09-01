package com.miniecommerce.shipping.app.port.inbound;

import com.miniecommerce.shipping.domain.ShippingMethod;

import java.util.List;

public interface ListShippingMethodsUseCase {

    List<ShippingMethod> listActive();
}