package org.apache.rocketmq.console.aspect.admin.annotation;

import java.lang.annotation.*;

/**
 * Created by tangjie
 * 2016/11/22
 * styletang.me@gmail.com
 * 在一个方法里调用了多次的MQAdmin的方法
 * 用这个注解防止频繁的连接/断开
 */
@Target({ ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MultiMQAdminCmdMethod {
}
