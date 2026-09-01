package com.miniecommerce.payment.app.port.outbound;

import com.miniecommerce.payment.domain.PaymentMethod;

import java.util.List;

public interface PaymentMethodRepository {

    List<PaymentMethod> findActive();
}
