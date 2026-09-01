package com.miniecommerce.shipping.adapter.outbound.persistence.mapper;

import com.miniecommerce.shipping.adapter.outbound.persistence.ShippingMethodEntity;
import com.miniecommerce.shipping.domain.ShippingMethod;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ShippingMethodPersistenceMapper {

    ShippingMethod toDomain(ShippingMethodEntity entity);
}