package com.miniecommerce.common.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import java.util.List;

/** Parses the JSON string stored in the {@code filters} request key. */
public abstract class AbstractQueryParser implements QueryParser {

    private static final int MAX_FILTERS = 50;

    private final ObjectMapper objectMapper;

    protected AbstractQueryParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public final List<Filter> parse(String json) {
        if (json == null || json.isBlank()) return List.of();

        List<Filter> filters = readJson(json);
        filters.forEach(this::validateFilter);
        return filters.stream().map(this::normalizeFilter).toList();
    }

    /** Reads JSON and performs structural validation common to all query parsers. */
    private List<Filter> readJson(String json) {
        List<Filter> filters;
        try {
            filters = objectMapper.readValue(json, new TypeReference<List<Filter>>() { });
        } catch (JsonProcessingException ex) {
            throw invalid("Invalid filters JSON: " + ex.getOriginalMessage());
        }
        if (filters == null) return List.of();
        if (filters.size() > MAX_FILTERS) throw invalid("Too many filters (max " + MAX_FILTERS + ")");

        for (Filter filter : filters) {
            if (filter == null) throw invalid("Filter must not be null");
            if (filter.field() == null || filter.field().isBlank()) throw invalid("Filter field is required");
            if (filter.operator() == null) throw invalid("Unsupported operator for field: " + filter.field());
            if (!filter.isValid()) throw invalid("value/values không khớp với operator: " + filter.operator());
        }
        return filters;
    }

    /** Service-specific validation after common structural validation. */
    protected void validateFilter(Filter filter) { }

    /** Service-specific normalization after validation. */
    protected Filter normalizeFilter(Filter filter) { return filter; }

    protected static AppException invalid(String message) {
        return new AppException(ErrorCode.BAD_REQUEST, message);
    }
}
