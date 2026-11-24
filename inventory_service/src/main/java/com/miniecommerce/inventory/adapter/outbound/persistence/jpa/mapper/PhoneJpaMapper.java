package com.miniecommerce.inventory.adapter.outbound.persistence.jpa.mapper;

import com.miniecommerce.inventory.adapter.outbound.persistence.jpa.entity.PhoneDraftJpaEntity;
import com.miniecommerce.inventory.adapter.outbound.persistence.jpa.entity.PhoneJpaEntity;
import com.miniecommerce.inventory.domain.Phone;
import com.miniecommerce.inventory.domain.PhoneDraft;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface PhoneJpaMapper {

    PhoneDraft toDomain(PhoneDraftJpaEntity entity);

    @Mapping(target = "importId", source = "importId")
    PhoneDraftJpaEntity toEntity(Long importId, PhoneDraft draft);

    Phone toDomain(PhoneJpaEntity entity);

    PhoneJpaEntity toEntity(Phone phone);


}
