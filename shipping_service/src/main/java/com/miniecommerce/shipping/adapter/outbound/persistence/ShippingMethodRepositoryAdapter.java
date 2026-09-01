package com.miniecommerce.shipping.adapter.outbound.persistence;

import com.miniecommerce.shipping.adapter.outbound.persistence.mapper.ShippingMethodPersistenceMapper;
import com.miniecommerce.shipping.app.port.outbound.ShippingMethodRepository;
import com.miniecommerce.shipping.domain.ShippingMethod;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ShippingMethodRepositoryAdapter implements ShippingMethodRepository {

    private final ShippingMethodJpa jpa;
    private final ShippingMethodPersistenceMapper mapper;

    public ShippingMethodRepositoryAdapter(ShippingMethodJpa jpa, ShippingMethodPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public List<ShippingMethod> findActive() {
        return jpa.findByActiveTrue().stream()
                .map(mapper::toDomain)
                .toList();
    }
}