package com.miniecommerce.shipping.app.service;

import com.miniecommerce.shipping.app.port.inbound.ListShippingMethodsUseCase;
import com.miniecommerce.shipping.app.port.outbound.ShippingMethodRepository;
import com.miniecommerce.shipping.domain.ShippingMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShippingMethodService implements ListShippingMethodsUseCase {

    private final ShippingMethodRepository repository;

    public ShippingMethodService(ShippingMethodRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingMethod> listActive() {
        return repository.findActive();
    }
}