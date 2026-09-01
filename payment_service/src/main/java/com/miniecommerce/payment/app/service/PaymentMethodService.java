package com.miniecommerce.payment.app.service;

import com.miniecommerce.payment.app.port.inbound.ListPaymentMethodsUseCase;
import com.miniecommerce.payment.app.port.outbound.PaymentMethodRepository;
import com.miniecommerce.payment.domain.PaymentMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentMethodService implements ListPaymentMethodsUseCase {

    private final PaymentMethodRepository repository;

    public PaymentMethodService(PaymentMethodRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethod> listActive() {
        return repository.findActive();
    }
}
