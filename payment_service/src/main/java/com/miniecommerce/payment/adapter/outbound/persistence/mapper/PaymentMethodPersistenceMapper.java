package com.miniecommerce.payment.adapter.outbound.persistence.mapper;

import com.miniecommerce.payment.adapter.outbound.persistence.PaymentMethodEntity;
import com.miniecommerce.payment.domain.PaymentMethod;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMethodPersistenceMapper {

    PaymentMethod toDomain(PaymentMethodEntity entity);
}