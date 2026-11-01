package com.miniecommerce.order.adapter.inbound.scheduling;

import com.miniecommerce.order.app.port.inbound.ExpirePendingOrdersUseCase;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

@DisallowConcurrentExecution
public class ExpirePendingOrdersJob extends QuartzJobBean {
    private static final Logger log = LoggerFactory.getLogger(ExpirePendingOrdersJob.class);

    @Autowired
    private ExpirePendingOrdersUseCase useCase;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        try {
            int expired = useCase.expirePendingOrders();
            log.info("Order expiration completed: instance={}, expired={}",
                    context.getScheduler().getSchedulerInstanceId(), expired);
        } catch (Exception ex) {
            // Next scheduled scan retries remaining PENDING orders; do not spin on immediate refires.
            throw new JobExecutionException("Failed to expire pending orders", ex);
        }
    }
}
