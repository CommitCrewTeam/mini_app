package com.miniecommerce.inventory.adapter.inbound.rest.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniecommerce.common.query.AbstractQueryParser;
import com.miniecommerce.common.query.Filter;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Parses and validates phone-import filters coming from the REST adapter. */
@Component
public class PhoneImportQueryParser extends AbstractQueryParser {

    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "id", "status", "mode", "version", "error", "createdAt", "updatedAt");
    private static final Set<String> UPPERCASE_FIELDS = Set.of("status", "mode");

    public PhoneImportQueryParser(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    protected void validateFilter(Filter filter) {
        if (!ALLOWED_FIELDS.contains(filter.field())) {
            throw invalid("Unsupported filter field: " + filter.field());
        }
    }

    @Override
    protected Filter normalizeFilter(Filter filter) {
        if (!UPPERCASE_FIELDS.contains(filter.field())) return filter;
        return new Filter(
                filter.field(),
                filter.operator(),
                upper(filter.value()),
                filter.values() == null ? null : filter.values().stream().map(this::upper).toList());
    }

    private String upper(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }
}
