package org.apache.rocketmq.console.stone.application;

/**
 * Created by songyongzhong on 2016/10/10.
 */

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
@EnableAutoConfiguration
abstract public class AbstractEmbeddedServerApplication {

    public static ConfigurableApplicationContext run(Object source, String[] args) {
        return SpringApplication.run(source, args);
    }

}
