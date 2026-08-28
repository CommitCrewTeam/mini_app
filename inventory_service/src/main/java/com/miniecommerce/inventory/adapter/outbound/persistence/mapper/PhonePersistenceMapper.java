package com.miniecommerce.inventory.adapter.outbound.persistence.mapper;

import com.miniecommerce.inventory.domain.Phone;
import com.miniecommerce.inventory.adapter.outbound.persistence.PhoneEntity;
import io.r2dbc.postgresql.codec.Json;
import org.mapstruct.Mapper;

import java.util.Map;

@Mapper(componentModel = "spring")
public interface PhonePersistenceMapper {

    Phone toDomain(PhoneEntity entity);

    PhoneEntity toEntity(Phone phone);

    default Json map(Map<String, Object> value) {
        return JsonMappingUtils.toJson(value);
    }

    default Map<String, Object> map(Json value) {
        return JsonMappingUtils.fromJson(value);
    }
}
