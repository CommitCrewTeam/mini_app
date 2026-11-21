package com.miniecommerce.inventory.domain.enums;

public enum PhoneImportStatus {
    /** Initial CSV ingestion and validation are running. */
    PENDING,
    /** Ingestion finished; one explicit validation or approval may start. */
    READY,
    /** The explicit validation job is running. */
    VALIDATING,
    /** Explicit validation passed; only approval may start. */
    VALIDATED,
    /** Approval validation and application are running. */
    APPROVING,
    /** Approval completed; terminal state. */
    DONE,
    /** CSV ingestion failed before the import became READY. */
    INGESTION_FAILED,
    /** Explicit validation failed, including invalid drafts or a failed job submission. */
    VALIDATION_FAILED,
    /** Approval failed during normal processing. */
    APPROVAL_FAILED,
    /** Approval was stopped because the validation gate found invalid drafts. */
    APPROVAL_VALIDATED_FAILED
}
