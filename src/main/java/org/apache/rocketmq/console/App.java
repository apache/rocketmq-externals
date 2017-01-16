package org.apache.rocketmq.console;

import org.apache.rocketmq.console.stone.application.AbstractEmbeddedServerApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by niuqinghua on 15/10/30.
 */
@Configuration
@ComponentScan(value = {"org.apache.rocketmq.console"})
public class App extends AbstractEmbeddedServerApplication {

    public static void main(String[] args) {
        run(App.class, args);
    }

}
