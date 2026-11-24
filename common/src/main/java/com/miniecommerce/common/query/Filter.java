package com.miniecommerce.common.query;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** One filter expression sent by the client. */
public record Filter(
        @NotBlank String field,
        @NotNull Op operator,
        String value,
        List<String> values) {

    @AssertTrue(message = "value/values không khớp với operator")
    public boolean isValid() {
        if (operator == null) return false;
        return switch (operator) {
            case IN, NOT_IN -> values != null && !values.isEmpty() && values.size() <= 100;
            case BETWEEN -> values != null && values.size() == 2;
            case IS_NULL, IS_NOT_NULL -> true;
            default -> value != null && !value.isEmpty();
        };
    }
}
