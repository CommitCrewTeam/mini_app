package com.miniecommerce.inventory.adapter.outbound.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
public class JsonMappingUtils {

    private final ObjectMapper mapper;

    public JsonMappingUtils(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Json toJson(Map<String, Object> value) {
        if (value == null) {
            return Json.of("{}");
        }
        try {
            return Json.of(mapper.writeValueAsString(value));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize 'detail' as JSON", e);
        }
    }

    public Map<String, Object> fromJson(Json value) {
        if (value == null) {
            return Collections.emptyMap();
        }
        try {
            return mapper.readValue(value.asString(), new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JSON content in 'detail' column", e);
        }
    }
}
