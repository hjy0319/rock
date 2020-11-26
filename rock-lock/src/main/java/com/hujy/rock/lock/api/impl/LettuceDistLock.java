package com.hujy.rock.lock.api.impl;

import com.hujy.rock.lock.api.DistLockApi;
import com.hujy.rock.lock.config.application.ApplicationContextConfig;
import com.hujy.rock.lock.exception.DistLockException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * lettuce实现分布式锁
 *
 * @author hujy
 * @version 1.0
 * @date 2019-10-19 10:49
 */
public class LettuceDistLock implements DistLockApi {
    /**
     * 释放锁的Lua脚本
     * requestId一致才能释放锁
     */
    private static final String UNLOCK_LUA = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    private static StringRedisTemplate stringRedisTemplate;

    private static DefaultRedisScript<Long> unlockLuaScript;


    static {
        stringRedisTemplate = ApplicationContextConfig.getClass(StringRedisTemplate.class);
        unlockLuaScript = new DefaultRedisScript<>();
        unlockLuaScript.setScriptText(UNLOCK_LUA);
        unlockLuaScript.setResultType(Long.class);
    }


    @Override
    public boolean lock(String lockKey, String requestId, int leaseTime, int waitTime, TimeUnit unit) throws DistLockException {
        long beginTime = System.currentTimeMillis();
        while (true) {
            // 申请获得锁
            boolean acquire = tryAcquire(lockKey, requestId, leaseTime, unit);
            if (acquire) {
                return true;
            }

            if (waitTime != -1) {
                long remainingWaitTime = unit.toMillis(waitTime) - (System.currentTimeMillis() - beginTime);
                // 如果waitTime到了，还没有获得锁，直接返回false
                if (remainingWaitTime <= 0L) {
                    return false;
                }
            }
            // 避免请求过于频繁
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new DistLockException("Lettuce加锁异常:" + e.getMessage(), e);
            }
        }
    }

    /**
     * 申请获得锁
     *
     * @param lockKey
     * @param requestId
     * @param expireTime
     * @param unit
     * @return boolean
     * @author hujy
     * @date 2019-10-21 11:12
     */
    private Boolean tryAcquire(String lockKey, String requestId, int expireTime, TimeUnit unit) {
        return stringRedisTemplate.opsForValue().setIfAbsent(lockKey, requestId, Duration.ofMillis(unit.toMillis(expireTime)));
    }

    @Override
    public boolean unlock(String lockKey, String requestId) {
        Object result = stringRedisTemplate.execute(unlockLuaScript, Collections.singletonList(lockKey), requestId);
        return Objects.equals(1L, result);
    }
}
