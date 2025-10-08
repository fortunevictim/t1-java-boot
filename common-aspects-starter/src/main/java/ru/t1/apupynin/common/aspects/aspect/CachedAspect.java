package ru.t1.apupynin.common.aspects.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Aspect
@RequiredArgsConstructor
public class CachedAspect {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Value("${t1.cache.ttl.ms:60000}")
    private long ttlMs;

    @Around("@annotation(cached)")
    public Object aroundCached(ProceedingJoinPoint pjp, ru.t1.apupynin.common.aspects.annotation.Cached cached) throws Throwable {
        Object[] args = pjp.getArgs();
        String cacheKey = buildKey(cached.cacheName(), args);

        CacheEntry existing = cache.get(cacheKey);
        if (existing != null && !existing.isExpired(ttlMs)) {
            log.info("CACHE_HIT key={}", cacheKey);
            return existing.value;
        }

        if (existing != null && existing.isExpired(ttlMs)) {
            log.info("CACHE_EXPIRED key={}", cacheKey);
        } else {
            log.info("CACHE_MISS key={}", cacheKey);
        }

        Object result = pjp.proceed();

        if (result != null) {
            cache.put(cacheKey, new CacheEntry(result));
            log.info("CACHE_PUT key={}", cacheKey);
        }
        return result;
    }

    private String buildKey(String cacheName, Object[] args) {
        StringBuilder sb = new StringBuilder(cacheName).append(":");
        if (args == null || args.length == 0) {
            return sb.append("noargs").toString();
        }
        for (Object arg : args) {
            if (arg == null) {
                sb.append("null").append('|');
            } else if (isPrimaryKeyCandidate(arg)) {
                sb.append(arg).append('|');
            } else {
                sb.append(Objects.hashCode(arg)).append('|');
            }
        }
        return sb.toString();
    }

    private boolean isPrimaryKeyCandidate(Object arg) {
        return arg instanceof Long || arg instanceof Integer || arg instanceof String;
    }

    private static class CacheEntry {
        final Object value;
        final long createdAtMillis;
        CacheEntry(Object value) {
            this.value = value;
            this.createdAtMillis = Instant.now().toEpochMilli();
        }
        boolean isExpired(long ttlMs) {
            return (Instant.now().toEpochMilli() - createdAtMillis) >= ttlMs;
        }
    }
}


