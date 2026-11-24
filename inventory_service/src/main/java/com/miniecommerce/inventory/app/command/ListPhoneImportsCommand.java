package com.miniecommerce.inventory.app.command;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.common.query.Filter;
import java.util.List;

/** Input model for listing phone imports, built and validated by the controller. */
public record ListPhoneImportsCommand(List<Filter> filters, int page, int size) {

    public static final int MAX_PAGE_SIZE = 100;

    public ListPhoneImportsCommand {
        if (page < 0) {
            throw new AppException(ErrorCode.BAD_REQUEST, "page must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "size must be between 1 and " + MAX_PAGE_SIZE);
        }
        filters = filters == null ? List.of() : List.copyOf(filters);
    }
}
