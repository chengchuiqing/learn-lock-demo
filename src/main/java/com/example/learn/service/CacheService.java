package com.example.learn.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public String getData(String key) {
        String cacheKey = "cache:" + key;
        String lockKey = "lock:" + key;
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);

        // 缓存穿透处理：缓存中不存在，数据库中也不存在，缓存空值
        if (cachedValue != null) {
            if ("null".equals(cachedValue)) {
                return null;
            } else {
                return cachedValue;
            }
        }

        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(10, 5, TimeUnit.SECONDS);
            if (acquired) {
                try {
                    // 再次检查缓存是否被其他线程更新
                    cachedValue = redisTemplate.opsForValue().get(cacheKey);
                    if (cachedValue != null) {
                        return cachedValue;
                    }

                    // 模拟查询数据库
                    String dbValue = queryFromDatabase(key);

                    if (dbValue == null) {
                        redisTemplate.opsForValue().set(cacheKey, "null", 10, TimeUnit.MINUTES);
                    } else {
                        int randomExpiration = new Random().nextInt(10); // 缓存雪崩处理：随机化过期时间
                        redisTemplate.opsForValue().set(cacheKey, dbValue, 30 + randomExpiration, TimeUnit.MINUTES);
                    }
                    return dbValue;
                } finally {
                    lock.unlock();
                }
            } else {
                // 未能获取锁，等待一段时间再尝试获取缓存
                Thread.sleep(50);
                return getData(key);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    // 模拟查询数据库
    private String queryFromDatabase(String key) {
        // 模拟数据库查询逻辑
        if ("validKey".equals(key)) {
            return "validValue";
        }
        return null;
    }
}