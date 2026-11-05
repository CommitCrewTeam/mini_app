package com.miniecommerce.inventory.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.miniecommerce.inventory.domain.enums.ImportMode;
import com.miniecommerce.inventory.domain.enums.PhoneImportStatus;
import java.nio.file.Path;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/** Shared data carriers for the phone import workflow. */
public final class PhoneImportPojo {
    private PhoneImportPojo() { }

    /** Only VALID rows count as valid; all draft statuses count toward total. */
    public record PhoneImportStat(
            @JsonProperty("total_count") long totalCount,
            @JsonProperty("fail_count") long failCount,
            @JsonProperty("valid_count") long validCount,
            ImportMode mode,
            PhoneImportStatus status) { }

    public record JobSubmission(Long importId, Long jobExecutionId) { }

    public record PhoneImportMatch(Long id, String key, Long version) { }

    /** Upload content supplied by the inbound adapter, without exposing its transport type. */
    public record PhoneImportUpload(
            String filename,
            ImportMode mode,
            Function<Path, Mono<Void>> transferTo
    ) { }

    public record PhoneImportSourceRow(long rowNumber, String key, String detail, String active,
            String stock, String errorMessage) { }
}
