package com.miniecommerce.payment.app.port.inbound;

import com.miniecommerce.payment.domain.PaymentMethod;

import java.util.List;

public interface ListPaymentMethodsUseCase {

    List<PaymentMethod> listActive();
}
