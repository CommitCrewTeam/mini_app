package com.miniecommerce.inventory.config;

import com.miniecommerce.inventory.app.job.BatchJobSubmitter;
import org.springframework.batch.core.launch.JobLauncher;
import com.zaxxer.hikari.HikariDataSource;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.batch.BatchDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import javax.sql.DataSource;

@Configuration
public class BatchInfrastructureConfiguration {
    @Bean
    public TaskExecutorJobLauncher batchImportJobLauncher(JobRepository repository,
            @Qualifier("batchTaskExecutor") ThreadPoolTaskExecutor executor) {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(repository);
        launcher.setTaskExecutor(executor);
        return launcher;
    }

    @Bean
    public JdbcTransactionManager batchTransactionManager(@Qualifier("batchDataSource") DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    @Bean
    public ThreadPoolTaskExecutor batchTaskExecutor(
            @Value("${app.batch-import.worker-count:2}") int workerCount,
            @Value("${app.batch-import.queue-capacity:20}") int queueCapacity,
            @Value("${app.batch-import.shutdown-wait-seconds:30}") int shutdownWaitSeconds) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(workerCount);
        executor.setMaxPoolSize(workerCount);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("batch-import-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(shutdownWaitSeconds);
        return executor;
    }

    @Bean
    public BatchJobSubmitter batchImportSubmitter(
            @Qualifier("batchImportJobLauncher") JobLauncher launcher) {
        return new BatchJobSubmitter(launcher);
    }

    @Bean
    public BatchJobSubmitter phoneApprovalSubmitter(
            @Qualifier("phoneApprovalJobLauncher") JobLauncher launcher) {
        return new BatchJobSubmitter(launcher);
    }

    @Bean
    @BatchDataSource
    @ConfigurationProperties("app.phone-import.datasource")
    public HikariDataSource batchDataSource() {
        return new HikariDataSource();
    }

    /** Dedicated launcher so approval jobs do not queue behind import/validation jobs. */
    @Bean
    public TaskExecutorJobLauncher phoneApprovalJobLauncher(JobRepository repository,
            @Qualifier("phoneApprovalJobExecutor") ThreadPoolTaskExecutor executor) {
        var launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(repository);
        launcher.setTaskExecutor(executor);
        return launcher;
    }

    @Bean
    public ThreadPoolTaskExecutor phoneApprovalJobExecutor(
            @Value("${app.phone-import.approval.launch-workers:5}") int workers,
            @Value("${app.phone-import.approval.launch-queue-capacity:20}") int queueCapacity) {
        if (workers < 1 || queueCapacity < 0) {
            throw new IllegalArgumentException("Approval launch workers must be positive and queue capacity nonnegative");
        }
        var executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("phone-approve-job-");
        executor.setCorePoolSize(workers);
        executor.setMaxPoolSize(workers);
        executor.setQueueCapacity(queueCapacity);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    @Bean
    public TransactionalOperator inventoryTransactions(ConnectionFactory connectionFactory) {
        return TransactionalOperator.create(new R2dbcTransactionManager(connectionFactory));
    }

}
