package com.hujy.rock.test.service.impl;

import com.hujy.rock.lock.annotation.DistLock;
import com.hujy.rock.lock.constant.DistLockType;
import com.hujy.rock.test.model.param.TestParam;
import com.hujy.rock.test.service.LockService;
import org.springframework.stereotype.Service;

/**
 * Description
 *
 * @author hujy
 * @version 1.0
 * @date 2020-11-18 17:34
 */
@Service
public class LockServiceImpl implements LockService {

    public static int total = 0;

    @Override
    public void incr(String id) {
        total ++;
    }

    @Override
    @DistLock(lockType = DistLockType.REDISSON, lockKey = "#id", waitTime = 60, expireTime = 60)
    public void incrByRedisson(String id) {
        total ++;
    }

    @Override
    @DistLock(lockType = DistLockType.JEDIS, lockKey = "#id", waitTime = 60, expireTime = 60)
    public void incrByJedis(String id) {
        total ++;
    }

    @Override
    @DistLock(lockType = DistLockType.REDISSON, lockKey = "#t.id", waitTime = 60, expireTime = 60)
    public void testByRedisson(TestParam t) {
        try {
            Thread.sleep(t.getNum());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    @DistLock(lockType = DistLockType.JEDIS, lockKey = "#t.id", waitTime = 60, expireTime = 60)
    public void testByJedis(TestParam t) {
        try {
            Thread.sleep(t.getNum());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
