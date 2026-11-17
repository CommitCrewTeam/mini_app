package com.miniecommerce.inventory.adapter.outbound.mapper;

import com.miniecommerce.inventory.domain.Phone;
import com.miniecommerce.inventory.adapter.outbound.persistence.phone.entity.PhoneEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = JsonMappingUtils.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface PhonePersistenceMapper {

    Phone toDomain(PhoneEntity entity);

    @Mapping(target = "name", source = "key")
    PhoneEntity toEntity(Phone phone);

}
