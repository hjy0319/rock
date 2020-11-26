package com.hujy.rock.test.service;

import com.hujy.rock.test.model.param.TestParam;

/**
 * Description
 *
 * @author hujy
 * @version 1.0
 * @date 2020-11-18 17:33
 */
public interface LockService {

    void incr(String id);

    void incrByRedisson(String id);

    void incrByJedis(String id);

    void testByRedisson(TestParam t);

    void testByJedis(TestParam t);

    void testByZookeeper(TestParam t);
}
