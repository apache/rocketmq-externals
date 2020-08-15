package org.apache.rocketmq.redis.test.processor;

import com.moilioncircle.redis.replicator.CloseListener;
import com.moilioncircle.redis.replicator.ExceptionListener;
import com.moilioncircle.redis.replicator.event.EventListener;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueString;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import java.io.IOException;
import org.apache.rocketmq.connect.redis.common.Config;
import org.apache.rocketmq.connect.redis.handler.DefaultRedisEventHandler;
import org.apache.rocketmq.connect.redis.handler.RedisEventHandler;
import org.apache.rocketmq.connect.redis.processor.DefaultRedisEventProcessor;
import org.apache.rocketmq.connect.redis.processor.RedisClosedListener;
import org.apache.rocketmq.connect.redis.processor.RedisEventListener;
import org.apache.rocketmq.connect.redis.processor.RedisEventProcessor;
import org.apache.rocketmq.connect.redis.processor.RedisExceptionListener;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ListenerTest {
    private Config config;
    private RedisEventProcessor processor;

    @Before
    public void init(){
        this.config = getConfig();
        this.processor = getProcessor();
        Assert.assertTrue(this.processor.isStopped());
    }

    @Test
    public void testEventListener(){
        EventListener eventListener = new RedisEventListener(this.config, processor);
        KeyValuePair pair = new KeyStringValueString();
        eventListener.onEvent(null, pair);
    }

    @Test
    public void testEventListenerConfigNull(){
        Exception ex = null;
        try {
            new RedisEventListener(null, processor);
        }catch (IllegalArgumentException e){
            ex = e;
        }
        Assert.assertNotNull(ex);
    }

    @Test
    public void testEventListenerRetry1(){
        Config config = getConfig();
        RedisEventProcessor processor = getFailedProcessor(config);

        EventListener eventListener = new RedisEventListener(config, processor);
        KeyValuePair pair = new KeyStringValueString();
        eventListener.onEvent(null, pair);

    }

    @Test
    public void testEventListenerRetry2(){
        Config config = getConfig();
        RedisEventProcessor processor = getExceptionProcessor(config);

        EventListener eventListener = new RedisEventListener(config, processor);
        KeyValuePair pair = new KeyStringValueString();
        eventListener.onEvent(null, pair);

    }

    @Test
    public void closeListenerTest(){
        CloseListener closeListener = new RedisClosedListener(processor);
        closeListener.handle(null);
    }

    @Test
    public void exceptionListenerTest(){
        ExceptionListener exceptionListener = new RedisExceptionListener(processor);
        KeyValuePair pair = new KeyStringValueString();
        exceptionListener.handle(null, new NullPointerException("adsf"), pair);
    }

    @After
    public void stop() throws IOException {
        this.processor.stop();
    }


    private RedisEventProcessor getProcessor(){
        Config config = getConfig();
        RedisEventHandler eventHandler = new DefaultRedisEventHandler(config);
        RedisEventProcessor processor = new DefaultRedisEventProcessor(config);
        processor.registEventHandler(eventHandler);
        return processor;
    }

    private RedisEventProcessor getFailedProcessor(Config config){
        RedisEventHandler eventHandler = new DefaultRedisEventHandler(config);
        RedisEventProcessor processor = mock(DefaultRedisEventProcessor.class);
        processor.registEventHandler(eventHandler);
        try {
            when(processor.commit(any())).thenReturn(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return processor;
    }

    private RedisEventProcessor getExceptionProcessor(Config config){
        RedisEventHandler eventHandler = new DefaultRedisEventHandler(config);
        RedisEventProcessor processor = mock(DefaultRedisEventProcessor.class);
        processor.registEventHandler(eventHandler);
        try {
            when(processor.commit(any())).thenThrow(new IllegalStateException("wrong number."));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return processor;
    }

    private Config getConfig(){
        Config config = new Config();

        config.setRedisAddr("127.0.0.1");
        config.setRedisPort(6379);
        return config;
    }
}
