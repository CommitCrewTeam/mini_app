package com.miniecommerce.inventory.adapter.outbound.mapper;

import com.miniecommerce.inventory.adapter.outbound.persistence.phone.PhoneDraftEntity;
import com.miniecommerce.inventory.adapter.outbound.persistence.phone.PhoneImportEntity;
import com.miniecommerce.inventory.domain.PhoneDraft;
import com.miniecommerce.inventory.domain.PhoneImport;
import java.util.UUID;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = JsonMappingUtils.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface PhoneImportPersistenceMapper {

    PhoneImport toDomain(PhoneImportEntity entity);

    PhoneDraft toDomain(PhoneDraftEntity entity);

    PhoneImportEntity toEntity(PhoneImport phoneImport);

    @Mapping(target = "importId", source = "importId")
    PhoneDraftEntity toEntity(UUID importId, PhoneDraft row);

}
