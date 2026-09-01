package com.miniecommerce.payment.adapter.outbound.persistence;

import com.miniecommerce.payment.adapter.outbound.persistence.mapper.PaymentMethodPersistenceMapper;
import com.miniecommerce.payment.app.port.outbound.PaymentMethodRepository;
import com.miniecommerce.payment.domain.PaymentMethod;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PaymentMethodRepositoryAdapter implements PaymentMethodRepository {

    private final PaymentMethodJpa jpa;
    private final PaymentMethodPersistenceMapper mapper;

    public PaymentMethodRepositoryAdapter(PaymentMethodJpa jpa, PaymentMethodPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public List<PaymentMethod> findActive() {
        return jpa.findByActiveTrue().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
