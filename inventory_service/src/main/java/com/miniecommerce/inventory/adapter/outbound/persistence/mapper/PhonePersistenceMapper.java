package com.miniecommerce.inventory.adapter.outbound.persistence.mapper;

import com.miniecommerce.inventory.domain.Phone;
import com.miniecommerce.inventory.domain.PhoneEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PhonePersistenceMapper {
    Phone toDomain(PhoneEntity entity);

    PhoneEntity toEntity(Phone phone);
}
