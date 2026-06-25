package com.intelligentdoctor.chat.service;

import com.intelligentdoctor.config.AppProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class StreamConcurrencyLimiter {

    private final AppProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final Semaphore localPermits;

    public StreamConcurrencyLimiter(AppProperties properties, StringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.localPermits = new Semaphore(properties.getStream().getMaxConcurrentRequests());
    }

    public Lease tryAcquire(String hospitalId) {
        if (!localPermits.tryAcquire()) {
            return null;
        }
        boolean globalAcquired = false;
        boolean hospitalAcquired = false;
        try {
            globalAcquired = tryAcquireRedisPermit("intelligent-doctor:stream:global");
            hospitalAcquired = globalAcquired && tryAcquireRedisPermit("intelligent-doctor:" + hospitalId + ":stream");
            if (globalAcquired && hospitalAcquired) {
                return new Lease(hospitalId, new AtomicBoolean(false));
            }
        } catch (Exception ex) {
            return new Lease(hospitalId, new AtomicBoolean(false));
        }
        if (globalAcquired) {
            decrement("intelligent-doctor:stream:global");
        }
        if (hospitalAcquired) {
            decrement("intelligent-doctor:" + hospitalId + ":stream");
        }
        localPermits.release();
        return null;
    }

    private boolean tryAcquireRedisPermit(String key) {
        Long value = redisTemplate.opsForValue().increment(key);
        if (value != null && value == 1L) {
            redisTemplate.expire(key, properties.getStream().getTimeoutMillis(), TimeUnit.MILLISECONDS);
        }
        if (value != null && value <= properties.getStream().getMaxConcurrentRequests()) {
            return true;
        }
        decrement(key);
        return false;
    }

    private void decrement(String key) {
        try {
            redisTemplate.opsForValue().decrement(key);
        } catch (Exception ignored) {
        }
    }

    public final class Lease implements AutoCloseable {
        private final String hospitalId;
        private final AtomicBoolean closed;

        private Lease(String hospitalId, AtomicBoolean closed) {
            this.hospitalId = hospitalId;
            this.closed = closed;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                localPermits.release();
                decrement("intelligent-doctor:stream:global");
                decrement("intelligent-doctor:" + hospitalId + ":stream");
            }
        }
    }
}
