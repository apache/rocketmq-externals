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

package org.apache.rocketmq.connect.jms.connector;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

import org.apache.rocketmq.connect.jms.Config;
import org.apache.rocketmq.connect.jms.ErrorCode;
import org.apache.rocketmq.connect.jms.Replicator;
import org.apache.rocketmq.connect.jms.pattern.PatternProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.connector.api.exception.DataConnectException;
import io.openmessaging.connector.api.source.SourceTask;

public abstract class BaseJmsSourceTask extends SourceTask {

    private static final Logger log = LoggerFactory.getLogger(BaseJmsSourceTask.class);

    private Replicator replicator;

    private Config config;

    private ByteBuffer sourcePartition;

    @Override
    public Collection<SourceDataEntry> poll() {
        List<SourceDataEntry> res = new ArrayList<>();
        try {
            Message message = replicator.getQueue().poll(1000, TimeUnit.MILLISECONDS);
            if (message != null) {
                Object[] payload = new Object[] {config.getDestinationType(), config.getDestinationName(), getMessageContent(message)};
                SourceDataEntry sourceDataEntry = new SourceDataEntry(sourcePartition, null, System.currentTimeMillis(), EntryType.CREATE, null, null, payload);
                res.add(sourceDataEntry);
            }
        } catch (Exception e) {
            log.error("activemq task poll error, current config:" + JSON.toJSONString(config), e);
        }
        return res;
    }

    @Override
    public void start(KeyValue props) {
        try {
            this.config = new Config();
            this.config.load(props);
            this.sourcePartition = ByteBuffer.wrap(config.getBrokerUrl().getBytes("UTF-8"));
            this.replicator = new Replicator(config,this);
            this.replicator.start();
        } catch (Exception e) {
            log.error("activemq task start failed.", e);
            throw new DataConnectException(ErrorCode.START_ERROR_CODE, e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        try {
            replicator.stop();
        } catch (Exception e) {
            log.error("activemq task stop failed.", e);
            throw new DataConnectException(ErrorCode.STOP_ERROR_CODE, e.getMessage(), e);
        }
    }

    @Override public void pause() {

    }

    @Override public void resume() {

    }

    @SuppressWarnings("unchecked")
    public ByteBuffer getMessageContent(Message message) throws JMSException {
        byte[] data = null;
        if (message instanceof TextMessage) {
            data = ((TextMessage) message).getText().getBytes();
        } else if (message instanceof ObjectMessage) {
            data = JSON.toJSONBytes(((ObjectMessage) message).getObject());
        } else if (message instanceof BytesMessage) {
            BytesMessage bytesMessage = (BytesMessage) message;
            data = new byte[(int) bytesMessage.getBodyLength()];
            bytesMessage.readBytes(data);
        } else if (message instanceof MapMessage) {
            MapMessage mapMessage = (MapMessage) message;
            Map<String, Object> map = new HashMap<>();
            Enumeration<Object> names = mapMessage.getMapNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement().toString();
                map.put(name, mapMessage.getObject(name));
            }
            data = JSON.toJSONBytes(map);
        } else if (message instanceof StreamMessage) {
            StreamMessage streamMessage = (StreamMessage) message;
            ByteArrayOutputStream bis = new ByteArrayOutputStream();
            byte[] by = new byte[1024];
            int i = 0;
            while ((i = streamMessage.readBytes(by)) != -1) {
                bis.write(by, 0, i);
            }
            data = bis.toByteArray();
        } else {
            // The exception is printed and does not need to be written as a DataConnectException
            throw new RuntimeException("message type exception");
        }
        return ByteBuffer.wrap(data);
    }
    
    public abstract PatternProcessor getPatternProcessor(Replicator replicator);
}
