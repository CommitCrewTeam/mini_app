package com.miniecommerce.inventory.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.miniecommerce.inventory.domain.enums.ImportMode;
import com.miniecommerce.inventory.domain.enums.PhoneImportStatus;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

/** Shared data carriers for the phone import workflow. */
public final class PhoneImportPojo {
    private PhoneImportPojo() { }

    /** WARNING rows are reported separately from both valid and invalid rows. */
    public record PhoneImportStat(
            @JsonProperty("total_count") long totalCount,
            @JsonProperty("fail_count") long failCount,
            @JsonProperty("valid_count") long validCount,
            @JsonProperty("warning_count") long warningCount,
            ImportMode mode,
            PhoneImportStatus status) { }

    public record PhoneDraftApprovalSummary(long totalDrafts, long validDrafts, long warningDrafts,
            long invalidDrafts) { }

    public record PhoneImportIngestionContext(PhoneImport phoneImport, Path inputFile) { }

    public record PhoneImportValidationContext(Long importId, ImportMode mode) { }

    public record PhoneImportApprovalContext(Long importId, ImportMode mode, boolean warningSkip) { }

    public record PhoneImportStatusTransition(
            List<PhoneImportStatus> allowedStatuses,
            PhoneImportStatus nextStatus) { }

    public record PhoneImportCASStatuses(
            PhoneImportStatus checkStatus,
            PhoneImportStatus successStatus,
            PhoneImportStatus failureStatus) { }

    /** One row of the phone import list. */
    public record PhoneImportListItem(
            Long id,
            PhoneImportStatus status,
            ImportMode mode,
            Long version,
            String error,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) { }

    /** Page of phone imports returned by the list endpoint. */
    public record PhoneImportPage(
            List<PhoneImportListItem> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {

        public static PhoneImportPage of(List<PhoneImportListItem> content, int page, int size,
                long totalElements) {
            int totalPages = (int) Math.ceil((double) totalElements / size);
            return new PhoneImportPage(content, page, size, totalElements, totalPages);
        }
    }

    public record PhoneImportMatch(Long id, String key, Long version) { }

    /** Raw CSV row, mirroring the phone_draft columns: rowNumber comes from the line position. */
    public record PhoneImportSourceRow(long rowNumber, String key, String detail, String quantity,
            String errorMessage) { }
}
