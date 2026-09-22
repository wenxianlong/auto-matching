package com.jn.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@ConditionalOnClass(RedisTemplate.class) // 同样加上条件注解
public class RedisUtils {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper; // 这里可以安全注入全局的 ObjectMapper

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public <T> T get(String key, Class<T> clazz) {
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj == null) return null;
        if (clazz.isInstance(obj)) return clazz.cast(obj);
        return objectMapper.convertValue(obj, clazz);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }
}

