/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.logstashplugin.output;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Event;
import org.apache.rocketmq.client.exception.MQClientException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.logstash.plugins.ConfigurationImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class RocketMQTest {
    static String namesvr = "127.0.0.1:9876";
    static String topic = "test";
    private static Map<String, Object> configValues = new HashMap<>();

    @BeforeClass
    public void init(){
        configValues.put(RocketMQ.CONFIG_NAMESRV_ADDR.name(), namesvr);
        configValues.put(RocketMQ.CONFIG_TOPIC.name(), topic);
    }

    @Test
    public void output() throws MQClientException {
        Configuration config = new ConfigurationImpl(configValues);
        RocketMQ output = new RocketMQ("test-oneway-send", config, null);

        String sourceField = "message";
        int eventCount = 10;
        Collection<Event> events = new ArrayList<>();
        for (int i = 0; i < eventCount; i++) {
            Event e = new org.logstash.Event();
            e.setField(sourceField, "message " + i);
            events.add(e);
        }

        output.output(events);
    }

//    public static void main(String[] args) throws MQClientException {
//        configValues.put(RocketMQ.CONFIG_NAMESRV_ADDR.name(), namesvr);
//        configValues.put(RocketMQ.CONFIG_TOPIC.name(), topic);
//        Configuration config = new ConfigurationImpl(configValues);
//        RocketMQ output = new RocketMQ("test-oneway-send", config, null);
//
//        String sourceField = "message";
//        int eventCount = 10;
//        Collection<Event> events = new ArrayList<>();
//        for (int i = 0; i < eventCount; i++) {
//            Event e = new org.logstash.Event();
//            e.setField(sourceField, "message " + i);
//            events.add(e);
//        }
//
//        output.output(events);
//    }
}