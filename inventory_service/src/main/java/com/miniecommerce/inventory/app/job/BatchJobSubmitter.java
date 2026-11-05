package com.miniecommerce.inventory.app.job;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.core.task.TaskRejectedException;

public final class BatchJobSubmitter {
    private static final Logger log = LoggerFactory.getLogger(BatchJobSubmitter.class);
    private final JobLauncher launcher;

    public BatchJobSubmitter(JobLauncher launcher) {
        this.launcher = launcher;
    }

    public JobExecution submit(Job job, JobParameters parameters, Runnable onLaunchFailure) {
        try {
            var execution = launcher.run(job, parameters);
            // A worker that started and failed quickly is still an accepted submission.
            if (execution.getStatus() == BatchStatus.FAILED && execution.getStartTime() == null) {
                throw new TaskRejectedException("Job submission failed: " + execution.getExitStatus().getExitDescription());
            }
            return execution;
        } catch (Exception error) {
            try {
                onLaunchFailure.run();
            } catch (Exception cleanupError) {
                if (cleanupError != error) {
                    error.addSuppressed(cleanupError);
                }
                log.error("Cannot update state after job launch failure", error);
            }
            throw toAppException(error);
        }
    }

    //acl
    private static AppException toAppException(Exception error) {
        if (error instanceof AppException appException) {
            return appException;
        }
        if (error instanceof TaskRejectedException) {
            return new AppException(ErrorCode.SERVICE_UNAVAILABLE, "System is busy, try again later", error);
        }
        if (error instanceof JobExecutionAlreadyRunningException) {
            return new AppException(ErrorCode.CONFLICT, "Job is already running", error);
        }
        if (error instanceof JobInstanceAlreadyCompleteException || error instanceof JobRestartException) {
            return new AppException(ErrorCode.CONFLICT, "Job was already executed with these parameters", error);
        }
        return new AppException(ErrorCode.INTERNAL_ERROR, "Cannot start job", error);
    }
}
