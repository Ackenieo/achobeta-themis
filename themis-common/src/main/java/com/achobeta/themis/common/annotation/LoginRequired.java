package com.achobeta.themis.common.annotation;

import java.lang.annotation.*;

/**
 * <p>
 * 描述：登录校验注解，标记需要校验登录的方法或类
 * </p>
 *
 * @Author: ZGjie20
 * @version: 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LoginRequired {

}
