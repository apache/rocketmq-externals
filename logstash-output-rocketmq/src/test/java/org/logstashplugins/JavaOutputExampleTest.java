package org.logstashplugins;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Event;
import org.apache.rocketmq.client.exception.MQClientException;
import org.junit.Before;
import org.junit.Test;
import org.logstash.plugins.ConfigurationImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class JavaOutputExampleTest {
    String namesvr = "127.0.0.1:9876";
    String topic = "test";
    Map<String, Object> configValues = new HashMap<>();


    @Before
    public void init(){
        configValues.put(RocketMQ.NAMESRV_CONFIG.name(), namesvr);
        configValues.put(RocketMQ.TOPIC_CONFIG.name(), topic);
    }

    @Test
    public void sendOneWayTest() throws MQClientException {
        Configuration config = new ConfigurationImpl(configValues);
        RocketMQ output = new RocketMQ("test-id", config, null);

        String sourceField = "message";
        int eventCount = 5;
        Collection<Event> events = new ArrayList<>();
        for (int k = 0; k < eventCount; k++) {
            Event e = new org.logstash.Event();
            e.setField(sourceField, "message " + k);
            events.add(e);
        }

        output.output(events);
    }

    @Test
    public void sendSyncTest() throws MQClientException {
        configValues.put(RocketMQ.SEND_CONFIG.name(), "sync");
        Configuration config = new ConfigurationImpl(configValues);
        RocketMQ output = new RocketMQ("test-id", config, null);

        String sourceField = "message";
        int eventCount = 5;
        Collection<Event> events = new ArrayList<>();
        for (int k = 0; k < eventCount; k++) {
            Event e = new org.logstash.Event();
            e.setField(sourceField, "message " + k);
            events.add(e);
        }

        output.output(events);
    }

    @Test
    public void sendAsyncTest() throws MQClientException {
        configValues.put(RocketMQ.SEND_CONFIG.name(), "async");
        Configuration config = new ConfigurationImpl(configValues);
        RocketMQ output = new RocketMQ("test-id", config, null);

        String sourceField = "message";
        int eventCount = 5;
        Collection<Event> events = new ArrayList<>();
        for (int k = 0; k < eventCount; k++) {
            Event e = new org.logstash.Event();
            e.setField(sourceField, "message " + k);
            events.add(e);
        }

        output.output(events);
    }
}
