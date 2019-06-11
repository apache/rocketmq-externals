package org.apache.rocketmq.connect.activemq.connector;

import static org.junit.Assert.assertEquals;

import org.apache.rocketmq.connect.activemq.Config;
import org.junit.Test;

import io.openmessaging.KeyValue;
import io.openmessaging.internal.DefaultKeyValue;

public class ActivemqConnectorTest {

    ActivemqSourceConnector connector = new ActivemqSourceConnector();

    @Test
    public void verifyAndSetConfigTest() {
        KeyValue keyValue = new DefaultKeyValue();

        for (String requestKey : Config.REQUEST_CONFIG) {
            assertEquals(connector.verifyAndSetConfig(keyValue), "Request config key: " + requestKey);
            keyValue.put(requestKey, requestKey);
        }
        assertEquals(connector.verifyAndSetConfig(keyValue), "");
    }

    @Test
    public void taskClassTest() {
        assertEquals(connector.taskClass(), ActivemqSourceTask.class);
    }

    @Test
    public void taskConfigsTest() {
        assertEquals(connector.taskConfigs().get(0), null);
        KeyValue keyValue = new DefaultKeyValue();
        for (String requestKey : Config.REQUEST_CONFIG) {
            keyValue.put(requestKey, requestKey);
        }
        connector.verifyAndSetConfig(keyValue);
        assertEquals(connector.taskConfigs().get(0), keyValue);
    }
}
