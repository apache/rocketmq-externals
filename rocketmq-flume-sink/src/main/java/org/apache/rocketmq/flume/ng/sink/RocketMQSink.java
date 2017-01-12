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
package org.apache.rocketmq.flume.ng.sink;

import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.MQProducer;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.common.message.Message;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Event;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class RocketMQSink extends AbstractSink implements Configurable {

    private static final Logger LOG = LoggerFactory.getLogger(RocketMQSink.class);

    private String topic;
    private String tag;
    private MQProducer producer;

    @Override
    public void configure(Context context) {

        topic = context.getString(RocketMQSinkUtil.TOPIC_CONFIG, RocketMQSinkUtil.TOPIC_DEFAULT);
        tag = context.getString(RocketMQSinkUtil.TAG_CONFIG, RocketMQSinkUtil.TAG_DEFAULT);

        producer = Preconditions.checkNotNull(RocketMQSinkUtil.getProducer(context));
    }

    @Override
    public Status process() throws EventDeliveryException {
        Channel channel = getChannel();
        Transaction tx = channel.getTransaction();
        try {
            tx.begin();
            Event event = channel.take();
            if (event == null || event.getBody() == null || event.getBody().length == 0) {
                tx.commit();
                return Status.READY;
            }

            SendResult sendResult = producer.send(new Message(topic, tag, event.getBody()));
            if (LOG.isDebugEnabled()) {
                LOG.debug("SendResult={}, Message={}", sendResult, event.getBody());
            }
            tx.commit();
            return Status.READY;
        } catch (Exception e) {
            LOG.error("RocketMQSink send message exception", e);
            try {
                tx.rollback();
                return Status.BACKOFF;
            } catch (Exception e2) {
                LOG.error("Rollback exception", e2);
            }
            return Status.BACKOFF;
        } finally {
            tx.close();
        }
    }

    @Override
    public synchronized void start() {
        try {

            producer.start();
        } catch (MQClientException e) {
            LOG.error("RocketMQSink start producer failed", e);
            Throwables.propagate(e);
        }
        super.start();
    }

    @Override
    public synchronized void stop() {

        producer.shutdown();
        super.stop();
    }
}
