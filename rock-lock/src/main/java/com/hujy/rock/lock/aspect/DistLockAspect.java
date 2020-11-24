package com.hujy.rock.lock.aspect;


import com.hujy.rock.common.extension.ExtensionLoader;
import com.hujy.rock.lock.annotation.DistLock;
import com.hujy.rock.lock.api.DistLockApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * 切面类
 *
 * @author hujy
 * @date 2019-10-21 10:47
 */
@Slf4j
@Configuration
@Aspect
@EnableAspectJAutoProxy
public class DistLockAspect {

    /**
     * 定义切点注解
     *
     * @param
     * @return void
     * @author hujy
     * @date 2019-10-21 10:54
     */
    @Pointcut("@annotation(com.hujy.rock.lock.annotation.DistLock)")
    public void distLockPointcut() {
    }

    /**
     * 定义环绕通知
     *
     * @param pjp
     * @return java.lang.Object
     * @author hujy
     * @date 2019-10-21 10:54
     */
    @Around("distLockPointcut()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        // 获取切点所在方法
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        // 根据注解解析key
        final String lockKey = getLockKey(method, pjp.getArgs());
        return startLock(lockKey, pjp, method);
    }

    private Object startLock(final String lockKey, ProceedingJoinPoint pjp, Method method) throws Throwable {
        DistLock annotation = method.getAnnotation(DistLock.class);
        // 每个请求的唯一标识
        String requestId = UUID.randomUUID().toString();
        return lock(pjp, annotation, lockKey, requestId);
    }

    /**
     * lock流程
     *
     * @param pjp
     * @param annotation
     * @param lockKey
     * @param requestId
     * @return java.lang.Object
     * @author hujy
     * @date 2019-10-21 10:55
     */
    private Object lock(ProceedingJoinPoint pjp, DistLock annotation, String lockKey, String requestId) throws Throwable {
        // 锁失效时间
        int expireTime = annotation.expireTime();
        // 锁等待时间
        int waitTime = annotation.waitTime();
        // 时间单位
        TimeUnit timeUnit = annotation.timeUnit();
        // 实现类型
        String lockType = annotation.lockType();
        //获得分布式锁的具体实现对象
        DistLockApi distLock = getLockImpl(lockType);

        try {
            boolean permit = distLock.lock(lockKey, requestId, expireTime, waitTime, timeUnit);
            if (permit) {
                // 执行具体业务逻辑
                return pjp.proceed();
            }
            return null;

        } finally {
            // 释放锁
            distLock.unlock(lockKey, requestId);
        }
    }

    /**
     * 组装分布式锁key
     *
     * @param method
     * @param args
     * @return java.lang.String
     * @author hujy
     * @date 2019-10-21 10:49
     */
    private String getLockKey(Method method, Object[] args) {
        Objects.requireNonNull(method);
        DistLock annotation = method.getAnnotation(DistLock.class);
        // 获取lockKey，解析Spring EL
        String lockKey = annotation.lockKey();
        lockKey = lockKey.startsWith("#") ? parseKey(annotation.lockKey(), method, args) : lockKey;
        // 获取分隔符
        String separator = annotation.separator();
        // 获取前缀
        String prefix = annotation.lockPrefix();
        // 获取后缀
        String suffix = annotation.lockSuffix();
        if (StringUtils.isBlank(lockKey)) {
            throw new IllegalArgumentException("lockKey can't be blank");
        }
        StringBuilder keyGenerator = new StringBuilder();
        // 前缀
        if (StringUtils.isNotBlank(prefix)) {
            keyGenerator.append(prefix).append(separator);
        }
        keyGenerator.append(lockKey.trim());
        // 后缀
        if (StringUtils.isNotBlank(suffix)) {
            keyGenerator.append(separator).append(suffix);
        }

        return keyGenerator.toString().trim();
    }


    /**
     * 解析key，支持Spring EL表达式
     *
     * @param key
     * @param method
     * @param args
     * @return java.lang.String
     * @author hujy
     * @date 2019-10-21 10:51
     */
    private String parseKey(String key, Method method, Object[] args) {
        // 获取被拦截方法参数名列表(使用Spring支持类库)
        LocalVariableTableParameterNameDiscoverer u =
                new LocalVariableTableParameterNameDiscoverer();
        String[] paraNameArr = u.getParameterNames(method);

        // 使用Spring EL进行key的解析
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();
        assert paraNameArr != null;
        for (int i = 0; i < paraNameArr.length; i++) {
            context.setVariable(paraNameArr[i], args[i]);
        }
        return Objects.requireNonNull(parser.parseExpression(key).getValue(context)).toString();
    }

    /**
     * 获取实现类对象
     * @author hujy
     * @date 2020-11-18 14:18
     * @param type
     * @return com.hujy.rock.lock.api.DistLockApi
     */
    private DistLockApi getLockImpl(String type) {
        log.info("lock type -> {}", type);
        return (DistLockApi) ExtensionLoader.getExtensionLoader(DistLockApi.class).getExtension(type);
    }
}
