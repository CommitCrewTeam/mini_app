package com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc.mapper;

import com.miniecommerce.inventory.domain.Phone;
import com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc.entity.PhoneEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = JsonMappingUtils.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface PhonePersistenceMapper {

    Phone toDomain(PhoneEntity entity);

    PhoneEntity toEntity(Phone phone);

}
