package org.apache.rocketmq.mqtt;

import org.apache.rocketmq.mqtt.common.MqttBridgeConfig;
import org.apache.rocketmq.mqtt.persistence.redis.RedisPool;
import org.apache.rocketmq.mqtt.persistence.redis.RedisService;
import org.junit.Before;
import org.junit.Test;

/**
 * @author chengxiangwang
 */
public class RedisServiceTest {

    private RedisService redisService;

    @Before
    public void init() {
        redisService = new RedisService(new RedisPool(new MqttBridgeController(new MqttBridgeConfig())));
    }

    @Test
    public void testGet_notExist() {

    }
}
