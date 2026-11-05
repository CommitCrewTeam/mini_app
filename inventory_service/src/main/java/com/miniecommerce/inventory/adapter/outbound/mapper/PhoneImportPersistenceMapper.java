package com.miniecommerce.inventory.adapter.outbound.mapper;

import com.miniecommerce.inventory.adapter.outbound.persistence.phone.entity.PhoneDraftEntity;
import com.miniecommerce.inventory.adapter.outbound.persistence.phone.entity.PhoneImportEntity;
import com.miniecommerce.inventory.domain.PhoneDraft;
import com.miniecommerce.inventory.domain.PhoneImport;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = JsonMappingUtils.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface PhoneImportPersistenceMapper {

    PhoneImport toDomain(PhoneImportEntity entity);

    PhoneDraft toDomain(PhoneDraftEntity entity);

    PhoneImportEntity toEntity(PhoneImport phoneImport);

    @Mapping(target = "importId", source = "importId")
    PhoneDraftEntity toEntity(Long importId, PhoneDraft row);

}
