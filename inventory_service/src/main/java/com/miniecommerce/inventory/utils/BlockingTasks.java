package com.miniecommerce.inventory.utils;

import java.util.concurrent.Callable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/** Runs blocking work away from WebFlux and R2DBC event-loop threads. */
@Component
public class BlockingTasks {

    private final Scheduler scheduler = Schedulers.boundedElastic();

    /** Moves downstream work onto a thread that may block, after reactive work upstream. */
    public <T> Mono<T> continueOnBlockingThread(Mono<T> upstream) {
        return upstream.publishOn(scheduler);
    }
}
