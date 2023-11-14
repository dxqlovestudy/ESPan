package com.xqcoder.easypan.annotation;

import org.springframework.web.bind.annotation.Mapping;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface GlobalInterceptor {
    /**
     * @description: 校验登录
     * @param
     * @return boolean
     * @author: HuaXian
     * @date: 2023/11/13 14:58
     */
    boolean checkLogin() default true;

    /**
     * @description: 校验参数
     * @param
     * @return boolean
     * @author: HuaXian
     * @date: 2023/11/13 14:58
     */
    boolean checkParams() default false;

    /**
     * @description: 校验管理员
     * @param
     * @return boolean
     * @author: HuaXian
     * @date: 2023/11/13 14:59
     */
    boolean checkAdmin() default false;
}
