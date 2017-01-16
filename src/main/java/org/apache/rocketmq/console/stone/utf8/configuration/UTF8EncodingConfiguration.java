package org.apache.rocketmq.console.stone.utf8.configuration;

/**
 * Created by songyongzhong on 2016/10/10.
 */
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CharacterEncodingFilter;

import javax.servlet.Filter;


@Configuration
public class UTF8EncodingConfiguration {

    @Bean(name="characterEncodingFilter")
    public Filter characterEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        return filter;
    }

}

