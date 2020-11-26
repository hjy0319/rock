package com.hujy.rock.test.controller;

import com.hujy.rock.test.model.param.TestParam;
import com.hujy.rock.test.service.LockService;
import com.hujy.rock.test.service.impl.LockServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Description
 *
 * @author hujy
 * @version 1.0
 * @date 2019-12-10 15:25
 */
@RestController
@RequestMapping
public class LockController {

    @Autowired
    private LockService lockService;

    private static final int DEFAULT_AMOUNT = 3000;

    @RequestMapping("/incr")
    public String incr(Integer mode, Integer a) {
        long start = System.currentTimeMillis();

        ExecutorService pool = Executors.newFixedThreadPool(16);
        LockServiceImpl.total = 0;
        a = Optional.ofNullable(a).orElse(DEFAULT_AMOUNT);
        if (mode == 0) {
            for (int i = 0; i < a; i++) {
                pool.execute(() -> lockService.incr("incr"));
            }
        } else if (mode == 1){
            for (int i = 0; i < a; i++) {
                pool.execute(() -> lockService.incrByRedisson("redisson"));
            }
        } else {
            for (int i = 0; i < a; i++) {
                pool.execute(() -> lockService.incrByJedis("jedis"));
            }
        }


        pool.shutdown();
        while (true) {
            if (pool.isTerminated()) {
                long used = System.currentTimeMillis() - start;
                String result = "total:" + LockServiceImpl.total + " used:" + used;
                System.out.println(result);
                return result;
            }
        }

    }

    @RequestMapping("/test")
    public String testTimeout(TestParam t) {
        if (t.getMode() == 1) {
            lockService.testByRedisson(t);
        } else if (t.getMode() == 2){
            lockService.testByJedis(t);
        } else {
            lockService.testByZookeeper(t);
        }

        return "ok";
    }
}
