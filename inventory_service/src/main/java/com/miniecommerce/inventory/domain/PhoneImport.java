package com.miniecommerce.inventory.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PhoneImport {

    private UUID id;
    private PhoneImportStatus status;
    private ImportMode mode;
    private Long version;
    private String message;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public PhoneImport() {
    }

    public PhoneImport(UUID id, PhoneImportStatus status, ImportMode mode, Long version,
            String message, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.status = status;
        this.mode = mode;
        this.version = version;
        this.message = message;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PhoneImport pending(UUID id, ImportMode mode) {
        var now = OffsetDateTime.now();
        return new PhoneImport(id, PhoneImportStatus.PENDING, mode, null, null, now, now);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void transitionTo(PhoneImportStatus next) {
        status = status.transitionTo(next);
        updatedAt = OffsetDateTime.now();
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
