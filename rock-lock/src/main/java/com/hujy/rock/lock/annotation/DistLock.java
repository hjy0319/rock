package com.hujy.rock.lock.annotation;


import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 * @author hujy
 * @date 2019-10-21 10:12
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistLock {

    /**
     * lock key
     */
    String lockKey();

    /**
     * lock key后缀
     */
    String lockPrefix() default "";

    /**
     * lock key前缀
     */
    String lockSuffix() default "";

    /**
     * 分隔符
     */
    String separator() default ":";

    /**
     * 实现策略的名称（默认使用Redisson）
     */
    String lockType() default "";

    /**
     * 是否使用乐观锁。
     */
    boolean optimistic() default false;

    /**
     * 锁最长等待时间。
     */
    int waitTime() default 5;

    /**
     * 锁过期时间。
     */
    int expireTime() default 30;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
