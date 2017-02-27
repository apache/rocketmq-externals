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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.jms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.ServiceThread;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.jms.exception.MessageExpiredException;
import org.apache.rocketmq.jms.hook.ReceiveMessageHook;
import org.apache.rocketmq.jms.msg.convert.RMQ2JMSMessageConvert;
import org.apache.rocketmq.jms.support.JMSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

/**
 * Service deliver messages synchronous or asynchronous.
 */
public class DeliverMessageService extends ServiceThread {

    private static final Logger log = LoggerFactory.getLogger(DeliverMessageService.class);
    private static final AtomicLong COUNTER = new AtomicLong(0L);
    private static final int PULL_BATCH_SIZE = 100;

    private RocketMQConsumer consumer;
    private DefaultMQPullConsumer rocketMQPullConsumer;
    private Destination destination;
    private String consumerGroup;
    private String topicName;
    private ConsumeModel consumeModel = ConsumeModel.SYNC;

    /** only support RMQ subExpression */
    private String messageSelector;
    private ReceiveMessageHook hook = new ReceiveMessageHook();

    /**
     * If durable is true, consume message from the offset consumed last time.
     * Otherwise consume from the max offset
     */
    private boolean durable = false;

    private BlockingQueue<MessageWrapper> msgQueue = new ArrayBlockingQueue(PULL_BATCH_SIZE);
    private volatile boolean pause = true;
    private final long index = COUNTER.incrementAndGet();

    private Map<MessageQueue, Long> offsetMap = new HashMap();

    public DeliverMessageService(RocketMQConsumer consumer, Destination destination, String consumerGroup) {
        this.consumer = consumer;
        this.destination = destination;
        this.consumerGroup = consumerGroup;

        this.topicName = JMSUtils.getDestinationName(destination);

        createAndStartRocketMQPullConsumer();

        if (this.consumer.getSession().getConnection().isStarted()) {
            this.recover();
        }
        else {
            this.pause();
        }
    }

    private void createAndStartRocketMQPullConsumer() {
        final ClientConfig clientConfig = this.consumer.getSession().getConnection().getClientConfig();
        this.rocketMQPullConsumer = new DefaultMQPullConsumer(consumerGroup);
        this.rocketMQPullConsumer.setNamesrvAddr(clientConfig.getNamesrvAddr());
        this.rocketMQPullConsumer.setInstanceName(clientConfig.getInstanceName());

        try {
            this.rocketMQPullConsumer.start();
        }
        catch (MQClientException e) {
            throw new JMSRuntimeException("Fail to start RocketMQ pull consumer, error msg:%s", ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    public String getServiceName() {
        return DeliverMessageService.class.getSimpleName() + "-" + this.index;
    }

    @Override
    public void run() {
        while (!isStopped()) {
            if (pause) {
                this.waitForRunning(1000);
                continue;
            }

            try {
                pullMessage();
            }
            catch (InterruptedException e) {
                log.info("Pulling messages service has been interrupted");
            }
            catch (Exception e) {
                log.error("Error during pulling messages", e);
            }
        }
    }

    private void pullMessage() throws Exception {
        Set<MessageQueue> mqs = this.rocketMQPullConsumer.fetchSubscribeMessageQueues(this.topicName);
        for (MessageQueue mq : mqs) {
            Long offset = offsetMap.get(mq);
            if (offset == null) {
                offset = beginOffset(mq);
            }
            PullResult pullResult = this.rocketMQPullConsumer.pullBlockIfNotFound(mq, this.messageSelector, offset, PULL_BATCH_SIZE);

            switch (pullResult.getPullStatus()) {
                case FOUND:
                    List<MessageExt> msgs = pullResult.getMsgFoundList();
                    offsetMap.put(mq, pullResult.getMaxOffset());
                    for (MessageExt msg : msgs) {
                        handleMessage(msg, mq);
                    }
                    log.debug("Pull {} messages from topic:{},broker:{},queueId:{}", msgs.size(), mq.getTopic(), mq.getBrokerName(), mq.getQueueId());
                    break;
                case NO_NEW_MSG:
                case NO_MATCHED_MSG:
                    break;
                case OFFSET_ILLEGAL:
                    throw new JMSException("Error during pull message[reason:OFFSET_ILLEGAL]");
            }
        }
    }

    /**
     * Refer to {@link #durable}.
     *
     * @param mq message queue
     * @return offset
     * @throws MQClientException
     */
    private Long beginOffset(MessageQueue mq) throws MQClientException {
        return this.durable ? this.rocketMQPullConsumer.fetchConsumeOffset(mq, false) : this.rocketMQPullConsumer.maxOffset(mq);
    }

    /**
     * If {@link #consumeModel} is {@link ConsumeModel#ASYNC}, messages pulled from broker
     * are handled in {@link ConsumeMessageService} owned by its session.
     *
     * If {@link #consumeModel} is {@link ConsumeModel#SYNC}, messages pulled from broker are put
     * into a memory blocking queue, waiting for the {@link MessageConsumer#receive()}
     * using {@link BlockingQueue#poll()} to handle messages synchronous.
     *
     * @param msg to handle message
     * @throws InterruptedException
     * @throws JMSException
     */
    private void handleMessage(MessageExt msg, MessageQueue mq) throws InterruptedException, JMSException {
        Message jmsMessage = RMQ2JMSMessageConvert.convert(msg);

        try {
            hook.before(jmsMessage);
        }
        catch (MessageExpiredException e) {
            log.debug(e.getMessage());
        }

        final MessageWrapper wrapper = new MessageWrapper(jmsMessage, this.consumer, mq, msg.getQueueOffset());

        switch (this.consumeModel) {
            case SYNC:
                this.msgQueue.put(wrapper);
                break;
            case ASYNC:
                this.consumer.getSession().getConsumeMessageService().put(wrapper);
                break;
            default:
                throw new JMSException(format("Unsupported consume model[%s]", this.consumeModel));
        }
    }

    public void ack(MessageQueue mq, Long offset) throws JMSException {
        try {
            this.rocketMQPullConsumer.updateConsumeOffset(mq, offset);
        }
        catch (MQClientException e) {
            throw new JMSException(format("Fail to ack offset[mq:%s,offset:%s]", mq, offset));
        }
    }

    public MessageWrapper poll() throws JMSException {
        try {
            return this.msgQueue.take();
        }
        catch (InterruptedException e) {
            throw new JMSException(e.getMessage());
        }
    }

    public MessageWrapper poll(long timeout, TimeUnit timeUnit) throws JMSException {
        try {
            return this.msgQueue.poll(timeout, timeUnit);
        }
        catch (InterruptedException e) {
            throw new JMSException(e.getMessage());
        }
    }

    public void pause() {
        this.pause = true;
    }

    public void recover() {
        this.pause = false;
    }

    public void close() {
        log.info("Begin to close message delivery service:{}", getServiceName());

        this.stop();

        this.rocketMQPullConsumer.shutdown();

        this.shutdown(true);

        log.info("Success to close message delivery service:{}", getServiceName());
    }

    public void setMessageSelector(String messageSelector) {
        this.messageSelector = messageSelector;
    }

    public void setDurable(boolean durable) {
        this.durable = durable;
    }

    public void setConsumeModel(ConsumeModel consumeModel) {
        this.consumeModel = consumeModel;
    }
}
