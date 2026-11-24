package com.miniecommerce.common.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

/** Generic filter operators shared by every service. */
public enum Op {

    EQ, NE, GT, GTE, LT, LTE,
    CONTAINS, STARTS_WITH, ENDS_WITH,
    IN, NOT_IN, BETWEEN,
    IS_NULL, IS_NOT_NULL;

    /**
     * Accepts both {@code gte} and {@code GTE}. Unknown values are returned as
     * {@code null} so the service's QueryParser can report the error from the
     * inbound adapter instead of failing during JSON deserialization.
     */
    @JsonCreator
    public static Op from(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
