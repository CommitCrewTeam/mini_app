package com.miniecommerce.order.adapter.inbound.scheduling;

import com.miniecommerce.order.app.port.inbound.ExpirePendingOrdersUseCase;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.quartz.JobBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;

class QuartzClusterTest {
    @Test
    void twoJdbcSchedulersNeverExecuteTheSameJobKeyConcurrently() throws Exception {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:quartz-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("org/quartz/impl/jdbcjobstore/tables_postgres.sql"))
                .execute(dataSource);
        var firstStarted = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);
        var secondStarted = new CountDownLatch(1);
        var calls = new AtomicInteger();
        var active = new AtomicInteger();
        var maxActive = new AtomicInteger();
        ExpirePendingOrdersUseCase useCase = () -> {
            maxActive.accumulateAndGet(active.incrementAndGet(), Math::max);
            try {
                if (calls.incrementAndGet() == 1) {
                    firstStarted.countDown();
                    if (!releaseFirst.await(15, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting for test release");
                    }
                } else {
                    secondStarted.countDown();
                }
                return 0;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            } finally {
                active.decrementAndGet();
            }
        };
        var first = scheduler(dataSource, "node-1", useCase);
        var second = scheduler(dataSource, "node-2", useCase);
        try {
            var job = JobBuilder.newJob(ExpirePendingOrdersJob.class)
                    .withIdentity(OrderExpirationQuartzConfiguration.JOB_KEY).storeDurably().build();
            first.getScheduler().addJob(job, false);
            first.getScheduler().triggerJob(job.getKey());
            first.start();
            assertThat(firstStarted.await(10, TimeUnit.SECONDS)).isTrue();
            // The first node keeps its running job, but only node 2 may acquire the next trigger.
            first.getScheduler().standby();
            second.start();
            second.getScheduler().triggerJob(job.getKey());
            assertThat(secondStarted.await(1500, TimeUnit.MILLISECONDS)).isFalse();
            releaseFirst.countDown();
            assertThat(secondStarted.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(calls.get()).isEqualTo(2);
            assertThat(maxActive.get()).isEqualTo(1);
        } finally {
            releaseFirst.countDown();
            first.destroy();
            second.destroy();
        }
    }

    private SchedulerFactoryBean scheduler(DataSource dataSource, String instanceId,
                                            ExpirePendingOrdersUseCase useCase) throws Exception {
        var properties = new Properties();
        properties.setProperty("org.quartz.scheduler.instanceId", instanceId);
        properties.setProperty("org.quartz.jobStore.isClustered", "true");
        properties.setProperty("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        properties.setProperty("org.quartz.scheduler.idleWaitTime", "1000");
        properties.setProperty("org.quartz.threadPool.threadCount", "2");
        var factory = new SchedulerFactoryBean();
        factory.setSchedulerName("cluster-test");
        factory.setDataSource(dataSource);
        factory.setQuartzProperties(properties);
        factory.setWaitForJobsToCompleteOnShutdown(true);
        factory.setJobFactory((bundle, scheduler) -> {
            var job = new ExpirePendingOrdersJob();
            ReflectionTestUtils.setField(job, "useCase", useCase);
            return job;
        });
        factory.afterPropertiesSet();
        return factory;
    }
}
