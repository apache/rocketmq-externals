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

import com.alibaba.rocketmq.client.consumer.DefaultMQPullConsumer;
import com.alibaba.rocketmq.client.consumer.PullResult;
import com.alibaba.rocketmq.client.consumer.PullStatus;
import com.alibaba.rocketmq.client.exception.MQBrokerException;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.alibaba.rocketmq.common.message.MessageQueue;
import com.alibaba.rocketmq.common.protocol.heartbeat.MessageModel;
import com.alibaba.rocketmq.remoting.exception.RemotingException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Set;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Sink;
import org.apache.flume.Transaction;
import org.apache.flume.channel.MemoryChannel;
import org.apache.flume.conf.Configurables;
import org.apache.flume.event.EventBuilder;
import org.junit.Test;
import org.slf4j.Logger;

import static org.apache.rocketmq.flume.ng.sink.RocketMQSinkConstants.NAME_SERVER_CONFIG;
import static org.apache.rocketmq.flume.ng.sink.RocketMQSinkConstants.TAG_CONFIG;
import static org.apache.rocketmq.flume.ng.sink.RocketMQSinkConstants.TAG_DEFAULT;
import static org.apache.rocketmq.flume.ng.sink.RocketMQSinkConstants.TOPIC_DEFAULT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 *
 */
public class RocketMQSinkTest {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(RocketMQSinkTest.class);

    private String nameserver = "localhost:9876";

    private DefaultMQPullConsumer consumer;
    private String tag = TAG_DEFAULT + "_SINK_TEST";
    private String consumerGroup = "CONSUMER_GROUP_SINK_TEST";

    @Test
    public void testEvent() throws MQClientException, InterruptedException, EventDeliveryException, RemotingException, MQBrokerException, UnsupportedEncodingException {

        /*
        start sink
         */
        Context context = new Context();
        context.put(NAME_SERVER_CONFIG,nameserver);
        context.put(TAG_CONFIG, tag);
        RocketMQSink sink = new RocketMQSink();
        Configurables.configure(sink,context);
        MemoryChannel channel = new MemoryChannel();
        Configurables.configure(channel,context);
        sink.setChannel(channel);
        sink.start();

        /*
        mock flume source
         */
        String sendMsg = "\"Hello RocketMQ\"" + "," + DateFormatUtils.format(new Date(), "yyyy-MM-DD hh:mm:ss");
        Transaction tx = channel.getTransaction();
        tx.begin();
        Event event = EventBuilder.withBody(sendMsg.getBytes(), null);
        channel.put(event);
        tx.commit();
        tx.close();
        log.info("publish message : {}", sendMsg);
        Sink.Status status = sink.process();
        if (status == Sink.Status.BACKOFF) {
            fail("Error");
        }

        sink.stop();

        /*
        consumer message
         */
        consumer = new DefaultMQPullConsumer(consumerGroup);
        consumer.setNamesrvAddr("localhost:9876");
        consumer.setMessageModel(MessageModel.valueOf("BROADCASTING"));
        consumer.registerMessageQueueListener(TOPIC_DEFAULT, null);
        consumer.start();

        String receiveMsg = null;
        Set<MessageQueue> queues = consumer.fetchSubscribeMessageQueues(TOPIC_DEFAULT);
        for(MessageQueue queue : queues){
            long offset = getMessageQueueOffset(queue);
            PullResult pullResult = consumer.pull(queue, tag, offset, 32);

            if (pullResult.getPullStatus() == PullStatus.FOUND) {
                for(MessageExt message : pullResult.getMsgFoundList()){
                    byte[] body = message.getBody();
                    receiveMsg = new String(body,"UTF-8");
                    log.info("receive message : {}", receiveMsg);
                }

                long nextBeginOffset = pullResult.getNextBeginOffset();
                putMessageQueueOffset(queue,nextBeginOffset);
            }
        }
        /*
        wait for processQueueTable init
         */
        Thread.sleep(1000);

        consumer.shutdown();


        assertEquals(sendMsg,receiveMsg);
    }


    private long getMessageQueueOffset(MessageQueue queue) throws MQClientException {

        long offset = consumer.fetchConsumeOffset(queue,false);
        if (offset < 0) {
            offset = 0;
        }

        return offset;
    }

    private void putMessageQueueOffset(MessageQueue queue, long offset) throws MQClientException {
        consumer.updateConsumeOffset(queue,offset);
    }
}
