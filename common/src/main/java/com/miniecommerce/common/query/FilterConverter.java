package com.miniecommerce.common.query;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.springframework.data.relational.core.query.Criteria;

public final class FilterConverter {

    private FilterConverter() {
    }

    public static Criteria convertFilterToCriteria(
            Filter filter, Map<String, Class<?>> fieldTypes) {
        Criteria.CriteriaStep step = Criteria.where(filter.field());
        return switch (filter.operator()) {
            case EQ -> step.is(convertFilterValueToFieldType(filter, fieldTypes));
            case NE -> step.not(convertFilterValueToFieldType(filter, fieldTypes));
            case GT -> step.greaterThan(convertFilterValueToFieldType(filter, fieldTypes));
            case GTE -> step.greaterThanOrEquals(convertFilterValueToFieldType(filter, fieldTypes));
            case LT -> step.lessThan(convertFilterValueToFieldType(filter, fieldTypes));
            case LTE -> step.lessThanOrEquals(convertFilterValueToFieldType(filter, fieldTypes));
            case CONTAINS -> step.like("%" + filter.value() + "%");
            case STARTS_WITH -> step.like(filter.value() + "%");
            case ENDS_WITH -> step.like("%" + filter.value());
            case IN -> step.in(convertFilterValuesToFieldTypes(filter, fieldTypes));
            case NOT_IN -> step.notIn(convertFilterValuesToFieldTypes(filter, fieldTypes));
            case BETWEEN -> step.between(
                    convertFilterValueToFieldType(filter.field(), filter.values().get(0), fieldTypes),
                    convertFilterValueToFieldType(filter.field(), filter.values().get(1), fieldTypes));
            case IS_NULL -> step.isNull();
            case IS_NOT_NULL -> step.isNotNull();
        };
    }

    public static Object[] convertFilterValuesToFieldTypes(
            Filter filter, Map<String, Class<?>> fieldTypes) {
        return filter.values().stream()
                .map(value -> convertFilterValueToFieldType(filter.field(), value, fieldTypes))
                .toArray();
    }

    private static Object convertFilterValueToFieldType(
            Filter filter, Map<String, Class<?>> fieldTypes) {
        return convertFilterValueToFieldType(filter.field(), filter.value(), fieldTypes);
    }

    private static Object convertFilterValueToFieldType(
            String field, String rawValue, Map<String, Class<?>> fieldTypes) {
        Class<?> fieldType = fieldTypes.get(field);
        if (fieldType == null) {
            throw invalid("Unsupported filter field: " + field);
        }
        if (rawValue == null) {
            return null;
        }

        try {
            if (fieldType == Long.class) {
                return Long.valueOf(rawValue);
            }
            if (fieldType == OffsetDateTime.class) {
                return OffsetDateTime.parse(rawValue);
            }
            return rawValue;
        } catch (IllegalArgumentException | DateTimeParseException error) {
            throw invalid("Invalid filter value for field " + field + ": " + rawValue);
        }
    }

    private static AppException invalid(String message) {
        return new AppException(ErrorCode.BAD_REQUEST, message);
    }
}
