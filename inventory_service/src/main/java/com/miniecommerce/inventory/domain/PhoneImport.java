package com.miniecommerce.inventory.domain;

import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportStatusTransition;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportStatuses;
import com.miniecommerce.inventory.domain.enums.ImportMode;
import com.miniecommerce.inventory.domain.enums.PhoneImportStatus;
import java.time.OffsetDateTime;
import java.util.List;

public class PhoneImport {

    private Long id;
    private PhoneImportStatus status;
    private ImportMode mode;
    private Long version;
    private String error;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public PhoneImport() {
    }

    public PhoneImport(Long id, PhoneImportStatus status, ImportMode mode, Long version,
            String error, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.status = status;
        this.mode = mode;
        this.version = version;
        this.error = error;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PhoneImport pending(ImportMode mode) {
        var now = OffsetDateTime.now();
        return new PhoneImport(null, PhoneImportStatus.PENDING, mode, null, null, now, now);
    }

    public static PhoneImportStatusTransition validationStartTransition() {
        return new PhoneImportStatusTransition(
                List.of(PhoneImportStatus.READY), PhoneImportStatus.VALIDATING);
    }

    public static PhoneImportStatusTransition approvalStartTransition() {
        return new PhoneImportStatusTransition(
                List.of(PhoneImportStatus.READY, PhoneImportStatus.VALIDATED), PhoneImportStatus.APPROVING);
    }

    public static PhoneImportStatuses ingestionResultStatuses() {
        return new PhoneImportStatuses(
                PhoneImportStatus.PENDING, PhoneImportStatus.READY, PhoneImportStatus.INGESTION_FAILED);
    }

    public static PhoneImportStatuses validationResultStatuses() {
        return new PhoneImportStatuses(
                PhoneImportStatus.VALIDATING, PhoneImportStatus.VALIDATED, PhoneImportStatus.VALIDATION_FAILED);
    }

    public static PhoneImportStatuses approvalResultStatuses() {
        return new PhoneImportStatuses(
                PhoneImportStatus.APPROVING, PhoneImportStatus.DONE, PhoneImportStatus.APPROVAL_FAILED);
    }

    public static PhoneImportStatuses approvalValidationFailedStatuses() {
        return new PhoneImportStatuses(
                PhoneImportStatus.APPROVING, PhoneImportStatus.DONE,
                PhoneImportStatus.APPROVAL_VALIDATED_FAILED);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PhoneImportStatus getStatus() {
        return status;
    }

    // Used when mapping persisted state.
    public void setStatus(PhoneImportStatus status) {
        this.status = status;
    }

    public ImportMode getMode() {
        return mode;
    }

    public void setMode(ImportMode mode) {
        this.mode = mode;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
