package com.miniecommerce.order.adapter.inbound.rest.mapper;

import com.miniecommerce.order.adapter.inbound.rest.dto.OrderRequest;
import com.miniecommerce.order.domain.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderRestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Order toOrder(OrderRequest request);
}
