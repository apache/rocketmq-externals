package org.apache.rocketmq.console.support.annotation;

import java.lang.annotation.*;
import org.apache.rocketmq.console.support.JsonBodyExceptionResolver;
import org.apache.rocketmq.console.support.JsonBodyReturnValueProcessor;

/**
 * Created by tangjie
 * 2016/11/21
 * styletang.me@gmail.com
 * when success return JsonResult(data)
 * @see JsonBodyReturnValueProcessor
 * when error return JsonResult(errCode,errMsg)
 * @see JsonBodyExceptionResolver
 *
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonBody {

}
