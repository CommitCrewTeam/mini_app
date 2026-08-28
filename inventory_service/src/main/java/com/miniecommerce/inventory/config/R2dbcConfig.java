package com.miniecommerce.inventory.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

import java.util.Map;

@Configuration
public class R2dbcConfig {

    @Bean
    public JsonToMapConverter jsonToMapConverter(ObjectMapper objectMapper) {
        return new JsonToMapConverter(objectMapper);
    }

    @Bean
    public MapToJsonConverter mapToJsonConverter(ObjectMapper objectMapper) {
        return new MapToJsonConverter(objectMapper);
    }

    @ReadingConverter
    public static class JsonToMapConverter implements Converter<Json, Map<String, Object>> {

        private final ObjectMapper objectMapper;

        public JsonToMapConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Map<String, Object> convert(Json source) {
            if (source == null) {
                return Map.of();
            }
            try {
                return objectMapper.readValue(source.asString(), new TypeReference<Map<String, Object>>() {
                });
            } catch (Exception e) {
                throw new IllegalStateException("Invalid JSON content in 'detail' column", e);
            }
        }
    }

    @WritingConverter
    public static class MapToJsonConverter implements Converter<Map<String, Object>, Json> {

        private final ObjectMapper objectMapper;

        public MapToJsonConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Json convert(Map<String, Object> source) {
            try {
                return Json.of(objectMapper.writeValueAsString(source == null ? Map.of() : source));
            } catch (Exception e) {
                throw new IllegalStateException("Unable to serialize 'detail' as JSON", e);
            }
        }
    }
}
