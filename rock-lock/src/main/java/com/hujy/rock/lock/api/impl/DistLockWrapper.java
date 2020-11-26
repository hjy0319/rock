package com.hujy.rock.lock.api.impl;


import com.hujy.rock.lock.api.DistLockApi;
import com.hujy.rock.lock.exception.DistLockException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 实现类装饰器
 *
 * @author hujy
 * @date 2019-10-21 11:06
 */
@Slf4j
public class DistLockWrapper implements DistLockApi {

    private DistLockApi distLockApi;

    public DistLockWrapper(DistLockApi distLockApi) {
        this.distLockApi = distLockApi;
    }

    @Override
    public boolean lock(String lockKey, String requestId, int expireTime, int waitTime, TimeUnit unit) throws DistLockException {
        log.info("lock -> lockKey:{} requestId:{} expireTime:{} waitTime:{}", lockKey, requestId, expireTime, waitTime);
        return distLockApi.lock(lockKey, requestId, expireTime, waitTime, unit);
    }

    @Override
    public boolean unlock(String lockKey, String requestId) throws DistLockException {
        log.info("unlock -> lockKey:{} requestId:{}", lockKey, requestId);
        return distLockApi.unlock(lockKey, requestId);
    }
}
