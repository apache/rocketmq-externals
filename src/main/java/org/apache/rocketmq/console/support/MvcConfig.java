package org.apache.rocketmq.console.support;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * Created by tangjie
 * 2016/11/21
 * styletang.me@gmail.com
 */
@Configuration
public class MvcConfig extends WebMvcConfigurationSupport {
    @Bean
    public JsonBodyExceptionResolver jsonBodyExceptionResolver() {
        return new JsonBodyExceptionResolver();
    }
}
