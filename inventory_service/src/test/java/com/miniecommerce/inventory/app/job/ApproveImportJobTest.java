package com.miniecommerce.inventory.app.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneImportRepositoryPort;
import com.miniecommerce.inventory.app.port.outbound.phone.BlockingPhoneDraftRepositoryPort;
import com.miniecommerce.inventory.domain.PhoneImport;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneDraftApprovalSummary;
import com.miniecommerce.inventory.domain.PhoneImportPojo.PhoneImportApprovalContext;
import com.miniecommerce.inventory.domain.enums.ImportMode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.test.util.ReflectionTestUtils;

class ApproveImportJobTest {

    @Test
    void approvalSubmissionCarriesWarningSkipParameter() throws Exception {
        Job job = mock(Job.class);
        JobLauncher launcher = mock(JobLauncher.class);
        when(launcher.run(eq(job), any(JobParameters.class))).thenReturn(mock(JobExecution.class));
        var submitter = new BatchJobSubmitter(launcher);
        var approveImportJob = new ApproveImportJob(
                submitter, job, mock(BlockingPhoneImportRepositoryPort.class));

        approveImportJob.submit(context(false));
        approveImportJob.submit(context(false));
        approveImportJob.submit(context(true));
        approveImportJob.submit(context(true));
        approveImportJob.submit(context(false));

        ArgumentCaptor<JobParameters> parameters = ArgumentCaptor.forClass(JobParameters.class);
        verify(launcher, times(5)).run(eq(job), parameters.capture());
        List<JobParameters> submitted = parameters.getAllValues();
        assertEquals(List.of("false", "false", "true", "true", "false"),
                submitted.stream().map(p -> p.getString("warningSkip")).toList());
        assertEquals(5L, submitted.stream().map(p -> p.getString("attempt")).distinct().count());
        assertTrue(submitted.stream().allMatch(p -> p.getParameter("attempt").isIdentifying()));
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, 0, 0, false, VALIDATION_INVALID",
            "2, 1, 0, 1, false, VALIDATION_INVALID",
            "2, 0, 1, 1, true, VALIDATION_INVALID",
            "2, 1, 1, 0, false, WARNINGS_UNCONFIRMED",
            "2, 1, 1, 0, true, APPROVABLE",
            "2, 2, 0, 0, false, APPROVABLE"
    })
    void gateUsesOneOutcomeName(long total, long valid, long warnings, long invalid,
            boolean warningSkip, String expected) {
        var drafts = mock(BlockingPhoneDraftRepositoryPort.class);
        when(drafts.summarizeForApproval(10L)).thenReturn(new PhoneDraftApprovalSummary(total, valid, warnings, invalid));
        JobExecutionDecider gate = ReflectionTestUtils.invokeMethod(
                new ApproveImportJob.ApprovalConfiguration(), "approvalGate", drafts);
        var execution = new JobExecution(1L, new JobParametersBuilder()
                .addLong("importId", 10L).addString("warningSkip", Boolean.toString(warningSkip)).toJobParameters());

        assertEquals(expected, gate.decide(execution, null).getName());
    }

    @ParameterizedTest
    @CsvSource({
            "WARNINGS_UNCONFIRMED, COMPLETED",
            "VALIDATION_INVALID, COMPLETED",
            "COMPLETED, COMPLETED",
            "FAILED, FAILED"
    })
    void listenerPreservesBusinessOutcome(String exit, BatchStatus status) {
        var repository = mock(BlockingPhoneImportRepositoryPort.class);
        JobExecutionListener listener = ReflectionTestUtils.invokeMethod(
                new ApproveImportJob.ApprovalConfiguration(), "approvalListener", repository);
        var execution = new JobExecution(1L, new JobParametersBuilder().addLong("importId", 10L).toJobParameters());
        execution.setStatus(status);
        execution.setExitStatus(new ExitStatus(exit));

        listener.afterJob(execution);

        switch (exit) {
            case "WARNINGS_UNCONFIRMED" -> verify(repository).writeStatusResult(10L, false,
                    PhoneImport.approvalWarningConfirmationStatuses(), "Set warningSkip=true to approve warning drafts");
            case "VALIDATION_INVALID" -> verify(repository).writeStatusResult(10L, false,
                    PhoneImport.approvalValidationFailedStatuses(), "validate failed");
            default -> verify(repository).writeStatusResult(10L, status == BatchStatus.COMPLETED,
                    PhoneImport.approvalResultStatuses(), null);
        }
    }

    private static PhoneImportApprovalContext context(boolean warningSkip) {
        return new PhoneImportApprovalContext(10L, ImportMode.UPDATE, warningSkip);
    }
}
