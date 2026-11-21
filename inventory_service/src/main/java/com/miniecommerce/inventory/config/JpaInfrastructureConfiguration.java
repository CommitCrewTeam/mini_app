package com.miniecommerce.inventory.config;

import com.miniecommerce.inventory.app.job.BatchJobSubmitter;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.batch.BatchDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EntityScan("com.miniecommerce.inventory.adapter.outbound.persistence.jpa.entity")
@EnableJpaRepositories(
        basePackages = "com.miniecommerce.inventory.adapter.outbound.persistence.jpa",
        transactionManagerRef = "jpaTransactionManager")
public class JpaInfrastructureConfiguration {
    @Bean
    @BatchDataSource
    @Primary
    @ConfigurationProperties("app.phone-import.datasource")
    public HikariDataSource jpaDataSource() {
        return new HikariDataSource();
    }

    @Bean
    public JpaTransactionManager jpaTransactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean
    public ThreadPoolTaskExecutor batchTaskExecutor(
            @Value("${app.batch-import.worker-count:5}") int workerCount,
            @Value("${app.batch-import.queue-capacity:10}") int queueCapacity,
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
            JobRepository repository,
            @Qualifier("batchTaskExecutor") ThreadPoolTaskExecutor executor) throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(repository);
        launcher.setTaskExecutor(executor);
        launcher.afterPropertiesSet();
        return new BatchJobSubmitter(launcher);
    }
}
