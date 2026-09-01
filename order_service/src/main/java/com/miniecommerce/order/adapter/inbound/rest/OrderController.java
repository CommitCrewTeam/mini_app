package com.miniecommerce.order.adapter.inbound.rest;

import com.miniecommerce.common.response.ApiResponse;
import com.miniecommerce.order.adapter.inbound.rest.dto.OrderPreviewResponse;
import com.miniecommerce.order.adapter.inbound.rest.dto.OrderRequest;
import com.miniecommerce.order.adapter.inbound.rest.dto.OrderResponse;
import com.miniecommerce.order.adapter.inbound.rest.mapper.OrderRestMapper;
import com.miniecommerce.order.app.command.CreateOrderCommand;
import com.miniecommerce.order.app.port.inbound.CreateOrderUseCase;
import com.miniecommerce.order.app.port.inbound.PreviewOrderUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final PreviewOrderUseCase previewOrderUseCase;
    private final OrderRestMapper orderRestMapper;

    public OrderController(CreateOrderUseCase createOrderUseCase,
                           PreviewOrderUseCase previewOrderUseCase,
                           OrderRestMapper orderRestMapper) {
        this.createOrderUseCase = createOrderUseCase;
        this.previewOrderUseCase = previewOrderUseCase;
        this.orderRestMapper = orderRestMapper;
    }

    @PostMapping
    public ApiResponse<OrderResponse> placeOrder(@RequestBody OrderRequest request) {
        CreateOrderCommand command = orderRestMapper.toCommand(request);
        return ApiResponse.success(orderRestMapper.toResponse(createOrderUseCase.placeOrder(command)));
    }

    @PostMapping("/preview")
    public ApiResponse<OrderPreviewResponse> previewOrder(@RequestBody OrderRequest request) {
        return ApiResponse.success(orderRestMapper.toPreviewResponse(
                previewOrderUseCase.preview(orderRestMapper.toPreviewCommand(request))));
    }
}