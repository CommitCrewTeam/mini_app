package com.miniecommerce.inventory.utils;

import java.util.function.Consumer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/** Runs blocking work away from WebFlux and R2DBC event-loop threads. */
@Component
public class BlockingTasks {

    private final Scheduler scheduler = Schedulers.boundedElastic();

    /** Starts an independent blocking task without waiting for completion. */
    public void runInBackground(Runnable task, Consumer<Throwable> onError) {
        Mono.fromRunnable(task)
                .subscribeOn(scheduler)
                .subscribe(ignored -> { }, onError);
    }
}
