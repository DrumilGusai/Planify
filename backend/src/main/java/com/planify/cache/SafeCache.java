package com.planify.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class SafeCache {

    private static final String NULL_SENTINEL = "__NONE__";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public <T> T get(String key, TypeReference<T> type, long ttlMinutes, Supplier<T> loader) {
        try {
            String raw = redis.opsForValue().get(key);
            if (raw != null) {
                return objectMapper.readValue(raw, type);
            }
        } catch (Exception e) {
            log.warn("Redis read failed for key {}: {}", key, e.toString());
        }

        T loaded = loader.get();
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(loaded), Duration.ofMinutes(ttlMinutes));
        } catch (Exception e) {
            log.warn("Redis write failed for key {}: {}", key, e.toString());
        }
        return loaded;
    }

    public <T> T get(String key, Class<T> type, long ttlMinutes, Supplier<T> loader) {
        try {
            String raw = redis.opsForValue().get(key);
            if (raw != null) {
                return objectMapper.readValue(raw, type);
            }
        } catch (Exception e) {
            log.warn("Redis read failed for key {}: {}", key, e.toString());
        }

        T loaded = loader.get();
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(loaded), Duration.ofMinutes(ttlMinutes));
        } catch (Exception e) {
            log.warn("Redis write failed for key {}: {}", key, e.toString());
        }
        return loaded;
    }

    public <T> T getOrNull(String key, Class<T> type, long ttlMinutes, Supplier<T> loader) {
        try {
            String raw = redis.opsForValue().get(key);
            if (raw != null) {
                if (NULL_SENTINEL.equals(raw)) {
                    return null;
                }
                return objectMapper.readValue(raw, type);
            }
        } catch (Exception e) {
            log.warn("Redis read failed for key {}: {}", key, e.toString());
        }

        T loaded = loader.get();
        try {
            String value = loaded == null ? NULL_SENTINEL : objectMapper.writeValueAsString(loaded);
            redis.opsForValue().set(key, value, Duration.ofMinutes(ttlMinutes));
        } catch (Exception e) {
            log.warn("Redis write failed for key {}: {}", key, e.toString());
        }
        return loaded;
    }

    public void evict(String... keys) {
        if (keys == null || keys.length == 0) {
            return;
        }
        try {
            redis.delete(Arrays.asList(keys));
        } catch (Exception e) {
            log.warn("Redis evict failed: {}", e.toString());
        }
    }

    public long version(Long projectId) {
        try {
            String v = redis.opsForValue().get("tasks:ver:p" + projectId);
            return v == null ? 0L : Long.parseLong(v);
        } catch (Exception e) {
            log.warn("Redis version read failed for project {}: {}", projectId, e.toString());
            return System.nanoTime();
        }
    }

    public void bump(Long projectId) {
        try {
            redis.opsForValue().increment("tasks:ver:p" + projectId);
        } catch (Exception e) {
            log.warn("Redis version bump failed for project {}: {}", projectId, e.toString());
        }
    }
}
