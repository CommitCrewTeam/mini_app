package com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc.mapper;

import com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc.entity.PhoneDraftEntity;
import com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc.entity.PhoneImportEntity;
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


}
