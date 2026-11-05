package com.miniecommerce.inventory.domain;

import java.nio.file.Path;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/** Upload content supplied by the inbound adapter, without exposing its transport type. */
public record PhoneImportUpload(
        String filename,
        ImportMode mode,
        Function<Path, Mono<Void>> transferTo
) { }
