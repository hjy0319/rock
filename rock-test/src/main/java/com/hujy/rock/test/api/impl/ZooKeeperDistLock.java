package com.hujy.rock.test.api.impl;

import com.hujy.rock.lock.api.DistLockApi;
import com.hujy.rock.lock.exception.DistLockException;

import java.util.concurrent.TimeUnit;

/**
 * Description
 *
 * @author hujy
 * @version 1.0
 * @date 2020-11-24 16:16
 */
public class ZooKeeperDistLock implements DistLockApi {
    @Override
    public boolean lock(String lockKey, String requestId, int expireTime, int waitTime, TimeUnit unit) throws DistLockException {
        System.out.println("zookeeper lock");
        return false;
    }

    @Override
    public boolean unlock(String lockKey, String requestId) throws DistLockException {
        System.out.println("zookeeper unlock");
        return false;
    }
}
