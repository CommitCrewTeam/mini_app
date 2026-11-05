package com.miniecommerce.inventory.adapter.inbound.rest.mapper;

import com.miniecommerce.inventory.adapter.inbound.rest.dto.PhoneRequest;
import com.miniecommerce.inventory.domain.Phone;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PhoneRestMapper {
    // A request carries no id/version; both are assigned by persistence.
    @Mapping(target = "version", ignore = true)
    Phone toPhone(PhoneRequest request);
}
