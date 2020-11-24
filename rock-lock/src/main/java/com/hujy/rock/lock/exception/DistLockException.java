package com.hujy.rock.lock.exception;

/**
 * 分布式锁异常
 * @author hujy
 * @date 2019-10-21 11:01
 */
public class DistLockException extends Exception{

    public DistLockException() {
        super();
    }

    public DistLockException(String message) {
        super(message);
    }

    public DistLockException(String message, Throwable cause) {
        super(message, cause);
    }

    public DistLockException(Throwable cause) {
        super(cause);
    }

    protected DistLockException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
