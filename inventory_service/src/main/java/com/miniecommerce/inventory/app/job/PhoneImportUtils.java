package com.miniecommerce.inventory.app.job;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;

/** Shared names and execution checks for phone import jobs. */
public final class PhoneImportUtils {
    private PhoneImportUtils() { }

    public static final String UPLOAD_JOB = "phoneUploadJob";
    public static final String VALIDATE_JOB = "phoneValidateJob";
    public static final String APPROVE_JOB = "phoneApproveJob";

    public static final String UPLOAD_STEP = "phoneUploadStep";
    public static final String VALIDATE_STEP = "phoneImportValidateStep";
    public static final String APPROVE_STEP = "phoneApprovalStep";

    public static boolean jobCompleted(JobExecution execution) {
        return execution.getStatus() == BatchStatus.COMPLETED;
    }

    public static boolean stepCompleted(JobExecution execution, String stepName) {
        return execution.getStepExecutions().stream()
                .anyMatch(step -> stepName.equals(step.getStepName())
                        && step.getStatus() == BatchStatus.COMPLETED);
    }
}
