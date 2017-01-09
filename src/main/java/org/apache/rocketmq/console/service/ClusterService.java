package org.apache.rocketmq.console.service;

import java.util.Map;
import java.util.Properties;

/**
 * Created by tangjie
 * 2016/11/21
 * styletang.me@gmail.com
 */
public interface ClusterService {
    Map<String,Object> list();

    Properties getBrokerConfig(String brokerAddr);
}
