package com.example.learn.service;

import com.example.learn.pojo.User;
import com.example.learn.repository.UserRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@EnableAsync
public class CacheService2 {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserRepository userRepository;

    public String getData(String key) {
        String cacheKey = "cache:" + key;
        String lockKey = "lock:" + key;
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);

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
                    cachedValue = redisTemplate.opsForValue().get(cacheKey);
                    if (cachedValue != null) {
                        return cachedValue;
                    }

                    String dbValue = queryFromDatabase(key);

                    if (dbValue == null) {
                        redisTemplate.opsForValue().set(cacheKey, "null", 10, TimeUnit.MINUTES);
                    } else {
                        int randomExpiration = new Random().nextInt(10);
                        redisTemplate.opsForValue().set(cacheKey, dbValue, 30 + randomExpiration, TimeUnit.MINUTES);
                    }
                    return dbValue;
                } finally {
                    lock.unlock();
                }
            } else {
                Thread.sleep(50);
                return getData(key);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }


    /**
     * 延时双删
     * @param key
     * @param newValue
     */
    @Transactional
    public void updateData(String key, String newValue) {
        String cacheKey = "cache:" + key;
        String lockKey = "lock:" + key;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(10, 5, TimeUnit.SECONDS);
            if (acquired) {
                try {
                    // 先删除缓存
                    redisTemplate.delete(cacheKey);

                    // 更新数据库
                    boolean dbUpdateSuccess = updateDatabase(key, newValue);

                    if (dbUpdateSuccess) {
                        // 延时双删策略：延迟后再次删除缓存
                        deleteCacheWithDelay(cacheKey, 5);
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                Thread.sleep(50);
                updateData(key, newValue);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Async
    public void deleteCacheWithDelay(String cacheKey, int delayInSeconds) {
        try {
            TimeUnit.SECONDS.sleep(delayInSeconds);
            redisTemplate.delete(cacheKey);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 模拟更新数据库
    private boolean updateDatabase(String key, String newValue) {
        // 模拟数据库更新逻辑
        User user = userRepository.findById(Long.parseLong(key)).orElse(null);
        if (user != null) {
            user.setUsername(newValue);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    // 模拟查询数据库
    private String queryFromDatabase(String key) {
        User user = userRepository.findById(Long.parseLong(key)).orElse(null);
        return user != null ? user.getUsername() : null;
    }
}