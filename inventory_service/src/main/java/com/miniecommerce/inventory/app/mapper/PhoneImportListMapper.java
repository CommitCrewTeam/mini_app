package com.miniecommerce.inventory.app.mapper;

import com.miniecommerce.inventory.domain.PhoneImport;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportListItem;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface PhoneImportListMapper {

    PhoneImportListItem toListItem(PhoneImport phoneImport);
}
