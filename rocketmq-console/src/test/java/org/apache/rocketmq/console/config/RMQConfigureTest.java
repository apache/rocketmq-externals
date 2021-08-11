package org.apache.rocketmq.console.config;

import java.io.File;
import org.apache.rocketmq.common.topic.TopicValidator;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;

public class RMQConfigureTest {

    private RMQConfigure rmqConfigure = new RMQConfigure();

    @Test
    public void testSet() {
        rmqConfigure.setAccessKey("rocketmq");
        rmqConfigure.setSecretKey("12345678");
        rmqConfigure.setDataPath("/tmp/rocketmq-console/data/test");
        rmqConfigure.setEnableDashBoardCollect("true");
        rmqConfigure.setIsVIPChannel("true");
        rmqConfigure.setUseTLS(true);
        rmqConfigure.setLoginRequired(true);
        rmqConfigure.setMsgTrackTopicName(null);
        rmqConfigure.setNamesrvAddr("127.0.0.1:9876");
    }

    @Test
    public void testGet() {
        testSet();
        Assert.assertEquals(rmqConfigure.getAccessKey(), "rocketmq");
        Assert.assertEquals(rmqConfigure.getSecretKey(), "12345678");
        Assert.assertTrue(rmqConfigure.isACLEnabled());
        Assert.assertTrue(rmqConfigure.isUseTLS());
        Assert.assertEquals(rmqConfigure.getConsoleCollectData(), "/tmp/rocketmq-console/data/test" + File.separator + "dashboard");
        Assert.assertEquals(rmqConfigure.getRocketMqConsoleDataPath(), "/tmp/rocketmq-console/data/test");
        Assert.assertEquals(rmqConfigure.getIsVIPChannel(), "true");
        Assert.assertTrue(rmqConfigure.isEnableDashBoardCollect());
        Assert.assertTrue(rmqConfigure.isLoginRequired());
        Assert.assertEquals(rmqConfigure.getMsgTrackTopicNameOrDefault(), TopicValidator.RMQ_SYS_TRACE_TOPIC);
        Assert.assertEquals(rmqConfigure.getNamesrvAddr(), "127.0.0.1:9876");
        ErrorPageRegistrar registrar = rmqConfigure.errorPageRegistrar();
        registrar.registerErrorPages(new ErrorPageRegistry() {
            @Override
            public void addErrorPages(ErrorPage... errorPages) {

            }
        });
    }
}
