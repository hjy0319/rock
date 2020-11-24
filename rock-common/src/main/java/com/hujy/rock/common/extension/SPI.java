package com.hujy.rock.common.extension;

import java.lang.annotation.*;

/**
 * 扩展点注解
 *
 * @author hujy
 * @version 1.0
 * @date 2019-10-22 14:55
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SPI {
    String value() default "";
}
