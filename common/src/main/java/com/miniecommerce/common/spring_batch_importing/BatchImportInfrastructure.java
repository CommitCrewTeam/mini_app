package com.miniecommerce.common.spring_batch_importing;

import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;

/** Explicitly import this class; it is deliberately not component-scanned. */
public class BatchImportInfrastructure {
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
}
