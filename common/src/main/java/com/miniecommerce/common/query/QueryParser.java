package com.miniecommerce.common.query;

import java.util.List;

/** Parses the raw JSON filters string into generic {@link Filter} objects. */
public interface QueryParser {

    List<Filter> parse(String filters);
}
