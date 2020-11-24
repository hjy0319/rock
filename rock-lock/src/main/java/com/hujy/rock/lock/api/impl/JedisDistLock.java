package com.hujy.rock.lock.api.impl;


import com.hujy.rock.lock.api.DistLockApi;
import com.hujy.rock.lock.config.application.ApplicationContextConfig;
import com.hujy.rock.lock.exception.DistLockException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisCommands;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * jedis实现分布式锁
 *
 * @author hujy
 * @version 1.0
 * @date 2019-10-17 16:17
 */
public class JedisDistLock implements DistLockApi {
    private static final String LOCK_SUCCESS = "OK";
    private static final Long UNLOCK_SUCCESS = 1L;
    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "PX";
    private static final String UNLOCK_LUA = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";


    private StringRedisTemplate stringRedisTemplate;


    public JedisDistLock() {
        this.stringRedisTemplate = ApplicationContextConfig.getClass(StringRedisTemplate.class);
    }


    private boolean tryAcquire(String lockKey, String requestId, int expireTime, TimeUnit unit) {
        RedisCallback<String> callback = (connection) -> {
            JedisCommands commands = (JedisCommands) connection.getNativeConnection();
            return commands.set(lockKey, requestId, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, unit.toMillis(expireTime));
        };

        String result = stringRedisTemplate.execute(callback);
        return LOCK_SUCCESS.equals(result);
    }

    @Override
    public boolean lock(String lockKey, String requestId, int expireTime, int waitTime, TimeUnit unit) throws DistLockException {
        long beginTime = System.currentTimeMillis();
        while (true) {
            boolean acquire = tryAcquire(lockKey, requestId, expireTime, unit);
            if (acquire) {
                return true;
            }

            if (waitTime != -1) {
                long remainingWaitTime = unit.toMillis(waitTime) - (System.currentTimeMillis() - beginTime);
                if (remainingWaitTime <= 0L) {
                    return false;
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new DistLockException("jedis加锁异常:" + e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean unlock(String lockKey, String requestId) {
        RedisCallback<Long> callback = (connection) -> {
            Object nativeConnection = connection.getNativeConnection();
            // 集群模式和单机模式虽然执行脚本的方法一样，但是没有共同的接口，所以只能分开执行
            if (nativeConnection instanceof JedisCluster) {// 集群模式
                return (Long) ((JedisCluster) nativeConnection).eval(UNLOCK_LUA, Collections.singletonList(lockKey), Collections.singletonList(requestId));
            } else if (nativeConnection instanceof Jedis) {// 单机模式
                return (Long) ((Jedis) nativeConnection).eval(UNLOCK_LUA, Collections.singletonList(lockKey), Collections.singletonList(requestId));
            }

            return 0L;
        };
        Long result = stringRedisTemplate.execute(callback);
        return UNLOCK_SUCCESS.equals(result);

    }

}
