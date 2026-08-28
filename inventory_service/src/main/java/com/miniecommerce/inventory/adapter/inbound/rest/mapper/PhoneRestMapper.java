package com.miniecommerce.inventory.adapter.inbound.rest.mapper;

import com.miniecommerce.inventory.adapter.inbound.rest.dto.PhoneRequest;
import com.miniecommerce.inventory.domain.Phone;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PhoneRestMapper {
    Phone toPhone(PhoneRequest request);
}
