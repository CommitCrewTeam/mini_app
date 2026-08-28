package com.miniecommerce.inventory.adapter.outbound.persistence.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;

import java.util.Collections;
import java.util.List;
import java.util.Map;

final class JsonMappingUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonMappingUtils() {
    }

    static Json toJson(Map<String, Object> value) {
        if (value == null) {
            return Json.of("{}");
        }
        try {
            return Json.of(MAPPER.writeValueAsString(value));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize 'detail' as JSON", e);
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> fromJson(Json value) {
        if (value == null) {
            return Collections.emptyMap();
        }
        try {
            return MAPPER.readValue(value.asString(), new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JSON content in 'detail' column", e);
        }
    }
}
