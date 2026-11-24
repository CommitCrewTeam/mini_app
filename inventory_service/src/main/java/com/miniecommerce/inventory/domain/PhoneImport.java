package com.miniecommerce.inventory.domain;

import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportStatusTransition;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportCASStatuses;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneDraftApprovalSummary;
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

    public static PhoneImport uploading(ImportMode mode) {
        var now = OffsetDateTime.now();
        return new PhoneImport(null, PhoneImportStatus.UPLOAD_IN_PROGRESS, mode, null, null, now, now);
    }

    public static PhoneImportStatusTransition validationStartTransition() {
        return new PhoneImportStatusTransition(
                List.of(PhoneImportStatus.VALIDATE_SUCCEEDED), PhoneImportStatus.VALIDATE_IN_PROGRESS);
    }

    public static PhoneImportStatusTransition approvalStartTransition() {
        return new PhoneImportStatusTransition(
                List.of(PhoneImportStatus.VALIDATE_SUCCEEDED), PhoneImportStatus.APPROVE_IN_PROGRESS);
    }

    public static PhoneImportCASStatuses uploadResultStatuses() {
        return new PhoneImportCASStatuses(
                PhoneImportStatus.UPLOAD_IN_PROGRESS, PhoneImportStatus.VALIDATE_SUCCEEDED,
                PhoneImportStatus.UPLOAD_FAILED);
    }

    public static PhoneImportCASStatuses uploadValidationResultStatuses() {
        return new PhoneImportCASStatuses(
                PhoneImportStatus.UPLOAD_IN_PROGRESS, PhoneImportStatus.VALIDATE_SUCCEEDED,
                PhoneImportStatus.VALIDATE_FAILED);
    }

    public static PhoneImportCASStatuses validationResultStatuses() {
        return new PhoneImportCASStatuses(
                PhoneImportStatus.VALIDATE_IN_PROGRESS, PhoneImportStatus.VALIDATE_SUCCEEDED, PhoneImportStatus.VALIDATE_FAILED);
    }

    public static PhoneImportCASStatuses approvalResultStatuses() {
        return new PhoneImportCASStatuses(
                PhoneImportStatus.APPROVE_IN_PROGRESS, PhoneImportStatus.APPROVE_SUCCEEDED, PhoneImportStatus.APPROVE_FAILED);
    }

    public static PhoneImportCASStatuses approvalValidationFailedStatuses() {
        return new PhoneImportCASStatuses(
                PhoneImportStatus.APPROVE_IN_PROGRESS, PhoneImportStatus.APPROVE_SUCCEEDED,
                PhoneImportStatus.APPROVE_VALIDATION_FAILED);
    }

    public static PhoneImportCASStatuses approvalWarningConfirmationStatuses() {
        return new PhoneImportCASStatuses(
                PhoneImportStatus.APPROVE_IN_PROGRESS, PhoneImportStatus.APPROVE_SUCCEEDED, PhoneImportStatus.VALIDATE_SUCCEEDED);
    }

    /** After validation, an import needs at least one draft and no INVALID drafts. */
    public static boolean isValidationValid(PhoneDraftApprovalSummary summary) {
        return summary.totalDrafts() > 0 && summary.invalidDrafts() == 0;
    }

    /** WARNING drafts need explicit user acceptance before they are applied. */
    public static boolean isRequiresWarningConfirmation(PhoneDraftApprovalSummary summary) {
        return summary.warningDrafts() > 0;
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
