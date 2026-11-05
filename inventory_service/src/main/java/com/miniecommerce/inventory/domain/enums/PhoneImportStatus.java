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
    /** Processing failed or validation blocked approval; terminal state. */
    FAILED
}
