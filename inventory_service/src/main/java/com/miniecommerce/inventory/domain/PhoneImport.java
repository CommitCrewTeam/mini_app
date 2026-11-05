package com.miniecommerce.inventory.domain;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.inventory.domain.enums.ImportMode;
import com.miniecommerce.inventory.domain.enums.PhoneImportStatus;
import java.time.OffsetDateTime;
import java.util.Objects;

public class PhoneImport {

    private Long id;
    private PhoneImportStatus status;
    private ImportMode mode;
    private Long version;
    private String message;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public PhoneImport() {
    }

    public PhoneImport(Long id, PhoneImportStatus status, ImportMode mode, Long version,
            String message, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.status = status;
        this.mode = mode;
        this.version = version;
        this.message = message;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PhoneImport pending(ImportMode mode) {
        var now = OffsetDateTime.now();
        return new PhoneImport(null, PhoneImportStatus.PENDING, mode, null, null, now, now);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void startValidation() {
        if (!canTransitionTo(status, PhoneImportStatus.VALIDATING)) {
            throw new AppException(ErrorCode.CONFLICT, "Import is not ready");
        }
        transitionTo(PhoneImportStatus.VALIDATING);
    }

    public void completeValidation(boolean completed, boolean allValid) {
        transitionTo(completed && allValid ? PhoneImportStatus.VALIDATED : PhoneImportStatus.FAILED);
    }

    public void submissionFailed() {
        transitionTo(PhoneImportStatus.FAILED);
    }

    public void completeImport(boolean completed) {
        transitionTo(completed ? PhoneImportStatus.READY : PhoneImportStatus.FAILED);
    }

    public void startApproval() {
        if (!canTransitionTo(status, PhoneImportStatus.APPROVING)) {
            throw new AppException(ErrorCode.CONFLICT, "Import is not ready");
        }
        transitionTo(PhoneImportStatus.APPROVING);
    }

    public static StatusChange completeApproval(Long approvalVersion, PhoneImportStatus next) {
        return new StatusChange(PhoneImportStatus.APPROVING,
                Objects.requireNonNull(approvalVersion, "Approval version is required"),
                validateTransition(PhoneImportStatus.APPROVING, next), "Approval ownership changed");
    }

    /** Expected state and requested state for an atomic repository update. */
    public record StatusChange(PhoneImportStatus expectedStatus, Long expectedVersion,
            PhoneImportStatus nextStatus, String conflictMessage) {
        public void requireApplied(boolean updated) {
            if (!updated) {
                throw new AppException(ErrorCode.CONFLICT, conflictMessage);
            }
        }
    }

    public void transitionTo(PhoneImportStatus next) {
        status = validateTransition(status, next);
        updatedAt = OffsetDateTime.now();
    }

    public static PhoneImportStatus validateTransition(PhoneImportStatus current, PhoneImportStatus next) {
        if (!canTransitionTo(current, next)) {
            throw new IllegalStateException("Cannot transition " + current + " to " + next);
        }
        return next;
    }

    public static boolean canTransitionTo(PhoneImportStatus current, PhoneImportStatus next) {
        if (current == null || next == null) {
            return false;
        }
        return switch (current) {
            case PENDING -> next == PhoneImportStatus.READY || next == PhoneImportStatus.FAILED;
            case READY -> next == PhoneImportStatus.VALIDATING || next == PhoneImportStatus.APPROVING;
            case VALIDATING -> next == PhoneImportStatus.VALIDATED || next == PhoneImportStatus.FAILED;
            case VALIDATED -> next == PhoneImportStatus.APPROVING;
            case APPROVING -> next == PhoneImportStatus.DONE || next == PhoneImportStatus.FAILED;
            case DONE, FAILED -> false;
        };
    }

    public PhoneImportStatus getStatus() {
        return status;
    }

    // Used when constructing or restoring persisted state; business changes use transitionTo.
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
