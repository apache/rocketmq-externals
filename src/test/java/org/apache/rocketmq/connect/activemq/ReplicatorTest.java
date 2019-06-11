package org.apache.rocketmq.connect.activemq;

import java.lang.reflect.Field;

import javax.jms.Message;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.rocketmq.connect.activemq.pattern.PatternProcessor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.junit.Assert;

public class ReplicatorTest {

    Replicator replicator;

    PatternProcessor patternProcessor;

    Config config;

    @Before
    public void before() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        config = new Config();
        replicator = new Replicator(config);

        patternProcessor = Mockito.mock(PatternProcessor.class);

        Field processor = Replicator.class.getDeclaredField("processor");
        processor.setAccessible(true);
        processor.set(replicator, patternProcessor);
    }

    @Test(expected = RuntimeException.class)
    public void startTest() {
        replicator.start();
    }

    @Test
    public void stop() {
        replicator.stop();
        Mockito.verify(patternProcessor, Mockito.times(1)).stop();
    }

    @Test
    public void commitAddGetQueueTest() {
        Message message = new ActiveMQTextMessage();
        replicator.commit(message, false);
        Assert.assertEquals(replicator.getQueue().poll(), message);
    }

    @Test
    public void getConfigTest() {
        Assert.assertEquals(replicator.getConfig(), config);
    }
}
