package com.miniecommerce.inventory.domain.enums;

public enum PhoneImportStatus {
    /** Upload job is running. */
    UPLOAD_IN_PROGRESS,
    /** Upload job failed. */
    UPLOAD_FAILED,
    /** Explicit validation job is running. */
    VALIDATE_IN_PROGRESS,
    /** Explicit validation passed; only approval may start. */
    VALIDATE_SUCCEEDED,
    /** Explicit validation failed. */
    VALIDATE_FAILED,
    /** Approval validation and application are running. */
    APPROVE_IN_PROGRESS,
    /** Approval completed successfully; terminal state. */
    APPROVE_SUCCEEDED,
    /** Approval failed, including drafts rejected by its validation gate. */
    APPROVE_FAILED,
    /** Approval's validation gate rejected the drafts before applying any phone. */
    APPROVE_VALIDATION_FAILED
}
