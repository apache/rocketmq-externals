package org.apache.rocketmq.console.stone.application;

/**
 * Created by songyongzhong on 2016/10/10.
 */
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ComponentScan
@EnableAutoConfiguration
abstract public class AbstractExternalServerApplication<T> extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(getApplicationType());
    }

    abstract protected Class<T> getApplicationType();

}
