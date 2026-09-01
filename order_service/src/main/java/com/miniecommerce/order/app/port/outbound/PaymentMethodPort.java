package com.miniecommerce.order.app.port.outbound;

import com.miniecommerce.order.domain.PaymentOption;

import java.util.List;

public interface PaymentMethodPort {

    List<PaymentOption> findActive();
}