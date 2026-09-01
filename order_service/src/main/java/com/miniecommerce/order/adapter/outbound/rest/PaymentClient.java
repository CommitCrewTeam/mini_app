package com.miniecommerce.order.adapter.outbound.rest;

import com.miniecommerce.common.response.ApiResponse;
import com.miniecommerce.order.adapter.outbound.rest.dto.PaymentMethodDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "payment-service", url = "${app.payment.url:http://localhost:8088}")
public interface PaymentClient {

    @GetMapping("/api/v1/payment-methods")
    ApiResponse<List<PaymentMethodDto>> listActive();
}