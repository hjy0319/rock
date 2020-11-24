package com.hujy.rock.lock.api;


import com.hujy.rock.common.extension.SPI;
import com.hujy.rock.lock.constant.DistLockType;
import com.hujy.rock.lock.exception.DistLockException;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁扩展点接口
 * @author hujy
 * @date 2019-10-21 11:02
 */
@SPI(DistLockType.REDISSON)
public interface DistLockApi {

    /**
     * 加锁
     * @author hujy
     * @date 2019-10-21 11:03
     * @param lockKey
     * @param requestId
     * @param expireTime
     * @param waitTime
     * @param unit
     * @return boolean
     */
    boolean lock(String lockKey, String requestId, int expireTime, int waitTime, TimeUnit unit) throws DistLockException;

    /**
     * 释放锁
     * @author hujy
     * @date 2019-10-21 11:04
     * @param lockKey
     * @param requestId
     * @return void
     */
    boolean unlock(String lockKey, String requestId) throws DistLockException;
}
