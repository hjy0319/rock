package com.hujy.rock.lock.api.impl;


import com.hujy.rock.lock.api.DistLockApi;
import com.hujy.rock.lock.config.application.ApplicationContextConfig;
import com.hujy.rock.lock.exception.DistLockException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * redisson实现分布式锁
 *
 * @author hujy
 * @date 2019-10-21 11:15
 */
public class RedissonDistLock implements DistLockApi {

    private RedissonClient redissonClient;


    public RedissonDistLock() {
        this.redissonClient = ApplicationContextConfig.getClass(RedissonClient.class);
    }

    @Override
    public boolean lock(String lockKey, String requestId, int expireTime, int waitTime, TimeUnit unit) throws DistLockException {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, expireTime, unit);
        } catch (Exception e) {
            throw new DistLockException("redisson加锁异常:" + e.getMessage(), e);
        }
    }

    @Override
    public boolean unlock(String lockKey, String requestId) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            return true;
        }
        return false;
    }
}
