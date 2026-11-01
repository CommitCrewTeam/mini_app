package com.miniecommerce.order.adapter.inbound.scheduling;

import com.miniecommerce.order.app.config.OrderExpirationProperties;
import java.time.Clock;
import java.util.TimeZone;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OrderExpirationProperties.class)
public class OrderExpirationQuartzConfiguration {
    public static final JobKey JOB_KEY = new JobKey("expire-pending-orders", "orders");

    @Bean
    public Clock orderClock() {
        return Clock.systemUTC();
    }

    @Bean
    public JobDetail expirePendingOrdersJobDetail() {
        return JobBuilder.newJob(ExpirePendingOrdersJob.class)
                .withIdentity(JOB_KEY).storeDurably().requestRecovery().build();
    }

    @Bean
    public Trigger expirePendingOrdersTrigger(
            @Qualifier("expirePendingOrdersJobDetail") JobDetail job, OrderExpirationProperties properties) {
        return TriggerBuilder.newTrigger().withIdentity("expire-pending-orders-trigger", "orders")
                .forJob(job).withSchedule(CronScheduleBuilder.cronSchedule(properties.cron())
                        .inTimeZone(TimeZone.getTimeZone("UTC"))
                        .withMisfireHandlingInstructionDoNothing()).build();
    }
}
