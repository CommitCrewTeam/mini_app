package com.miniecommerce.order.adapter.inbound.rest;

import com.miniecommerce.common.response.ApiResponse;
import com.miniecommerce.order.adapter.inbound.rest.dto.OrderRequest;
import com.miniecommerce.order.adapter.inbound.rest.mapper.OrderRestMapper;
import com.miniecommerce.order.app.port.inbound.CreateOrderUseCase;
import com.miniecommerce.order.domain.Order;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final OrderRestMapper orderRestMapper;

    public OrderController(CreateOrderUseCase createOrderUseCase, OrderRestMapper orderRestMapper) {
        this.createOrderUseCase = createOrderUseCase;
        this.orderRestMapper = orderRestMapper;
    }

    @PostMapping
    public ApiResponse<Order> placeOrder(@RequestBody OrderRequest request) {
        Order order = orderRestMapper.toOrder(request);
        Order saved = createOrderUseCase.placeOrder(order);
        return ApiResponse.success(saved);
    }
}
