package com.miniecommerce.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Phiên bản reactive (non-blocking) của {@link RedisService}, dành cho các service
 * chạy full reactive stack (WebFlux + R2DBC, ví dụ inventory_service).
 *
 * API này KHÔNG dành cho service Spring MVC (Tomcat/blocking): trong pipeline blocking
 * hãy dùng RedisService; trong pipeline reactive tuyệt đối không gọi .block().
 */
@Service
@RequiredArgsConstructor
public class ReactiveRedisService {

    private final ReactiveStringRedisTemplate stringReactiveRedisTemplate;
    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    public Mono<Boolean> setString(String key, String value) {
        return stringReactiveRedisTemplate.opsForValue().set(key, value);
    }

    public Mono<String> getString(String key) {
        return stringReactiveRedisTemplate.opsForValue().get(key);
    }

    public Mono<Boolean> setStringWithTTL(String key, String value, long timeout, TimeUnit timeUnit) {
        return stringReactiveRedisTemplate.opsForValue().set(key, value, durationOf(timeout, timeUnit));
    }

    public Mono<Boolean> set(String key, Object value) {
        return reactiveRedisTemplate.opsForValue().set(key, value);
    }

    public Mono<Boolean> setWithTTL(String key, Object value, long timeout, TimeUnit timeUnit) {
        return reactiveRedisTemplate.opsForValue().set(key, value, durationOf(timeout, timeUnit));
    }

    public Mono<Object> get(String key) {
        return reactiveRedisTemplate.opsForValue().get(key);
    }

    public Mono<Boolean> delete(String key) {
        return reactiveRedisTemplate.delete(key)
                .map(deletedCount -> deletedCount > 0);
    }

    private Duration durationOf(long timeout, TimeUnit timeUnit) {
        return Duration.of(timeout, timeUnit.toChronoUnit());
    }
}
