package com.miniecommerce.payment.adapter.inbound.rest;

import com.miniecommerce.common.response.ApiResponse;
import com.miniecommerce.payment.app.port.inbound.ListPaymentMethodsUseCase;
import com.miniecommerce.payment.domain.PaymentMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payment-methods")
public class PaymentMethodController {

    private final ListPaymentMethodsUseCase listPaymentMethodsUseCase;

    public PaymentMethodController(ListPaymentMethodsUseCase listPaymentMethodsUseCase) {
        this.listPaymentMethodsUseCase = listPaymentMethodsUseCase;
    }

    @GetMapping
    public ApiResponse<List<PaymentMethod>> listActive() {
        return ApiResponse.success(listPaymentMethodsUseCase.listActive());
    }
}
