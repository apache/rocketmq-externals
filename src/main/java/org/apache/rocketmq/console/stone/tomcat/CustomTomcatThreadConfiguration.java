package org.apache.rocketmq.console.stone.tomcat;

/**
 * Created by songyongzhong on 2016/10/10.
 */
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomTomcatThreadConfiguration {

    @Value("${tomcat.maxThreads:500}")
    private int maxThreads;

    @Value("${tomcat.minSpareThreads:20}")
    private int minSpareThreads;

    @Value("${tomcat.backlog:1000}")
    private int backlog;

    @Value("${tomcat.keepAliveTimeout:60000}")
    private int keepAliveTimeout;

    @Value("${tomcat.maxKeepAliveRequests:500}")
    private int maxKeepAliveRequests;

    @Bean
    public EmbeddedServletContainerCustomizer embeddedServletContainerCustomizerThreadConfig() {
        return new EmbeddedServletContainerCustomizer() {
            @Override
            public void customize(ConfigurableEmbeddedServletContainer factory) {
                if (factory instanceof TomcatEmbeddedServletContainerFactory) {
                    TomcatEmbeddedServletContainerFactory tomcatFactory = (TomcatEmbeddedServletContainerFactory)
                            factory;
                    tomcatFactory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
                        @Override
                        public void customize(Connector connector) {
                            final Http11NioProtocol protocolHandler = (Http11NioProtocol) connector.getProtocolHandler();
                            protocolHandler.setMaxThreads(maxThreads);
                            protocolHandler.setMinSpareThreads(minSpareThreads);
                            protocolHandler.setBacklog(backlog);
                            protocolHandler.setKeepAliveTimeout(keepAliveTimeout);
                            protocolHandler.setMaxKeepAliveRequests(maxKeepAliveRequests);
                        }
                    });
                }
            }
        };
    }

}
