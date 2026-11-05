package com.miniecommerce.inventory.config;

import com.zaxxer.hikari.HikariDataSource;
import com.miniecommerce.common.spring_batch_importing.BatchImportInfrastructure;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.batch.BatchDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration
@Import(BatchImportInfrastructure.class)
public class BatchInfrastructureConfiguration {
    @Bean
    @BatchDataSource
    @ConfigurationProperties("app.phone-import.datasource")
    public HikariDataSource batchDataSource() {
        return new HikariDataSource();
    }

    /** Dedicated launcher so approval jobs do not queue behind import/validation jobs. */
    @Bean
    public TaskExecutorJobLauncher phoneApprovalJobLauncher(JobRepository repository,
            @Qualifier("phoneApprovalJobExecutor") SimpleAsyncTaskExecutor executor) {
        var launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(repository);
        launcher.setTaskExecutor(executor);
        return launcher;
    }

    @Bean
    public SimpleAsyncTaskExecutor phoneApprovalJobExecutor(
            @Value("${app.phone-import.approval.launch-workers:5}") int workers) {
        if (workers < 1) {
            throw new IllegalArgumentException("Approval launch workers must be positive");
        }
        var executor = new SimpleAsyncTaskExecutor("phone-approve-job-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(workers);
        return executor;
    }
}
