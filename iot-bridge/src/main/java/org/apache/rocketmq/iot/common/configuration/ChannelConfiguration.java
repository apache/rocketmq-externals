package org.apache.rocketmq.iot.common.configuration;

import io.netty.util.AttributeKey;

public class ChannelConfiguration {
    public static final String CHANNEL_IDLE_TIME = "channelIdleTime";
    public static final AttributeKey<Integer> CHANNEL_IDLE_TIME_ATTRIBUTE_KEY = AttributeKey.valueOf(CHANNEL_IDLE_TIME);
}
