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
package org.apache.rocketmq.mqtt.task;

import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.consumer.PullStatus;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.impl.consumer.PullResultExt;
import org.apache.rocketmq.common.filter.ExpressionType;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageDecoder;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.RequestCode;
import org.apache.rocketmq.common.protocol.ResponseCode;
import org.apache.rocketmq.common.protocol.header.GetMaxOffsetRequestHeader;
import org.apache.rocketmq.common.protocol.header.PullMessageRequestHeader;
import org.apache.rocketmq.common.protocol.header.PullMessageResponseHeader;
import org.apache.rocketmq.common.sysflag.PullSysFlag;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.client.MqttClientManagerImpl;
import org.apache.rocketmq.mqtt.common.Message;
import org.apache.rocketmq.mqtt.common.MqttSubscriptionData;
import org.apache.rocketmq.mqtt.persistence.service.PersistService;
import org.apache.rocketmq.mqtt.utils.MqttUtil;
import org.apache.rocketmq.remoting.exception.RemotingCommandException;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.rocketmq.mqtt.constant.MqttConstant.TOPIC_CLIENTID_SEPARATOR;

public class MqttPushOfflineMessageTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(MqttPushOfflineMessageTask.class);
    private final MqttBridgeController mqttBridgeController;
    private MQTTSession client;
    private String brokerName;
    private String rootTopic;

    public MqttPushOfflineMessageTask(MqttBridgeController mqttBridgeController, String rootTopic, MQTTSession client,
        String brokerName) {
        this.mqttBridgeController = mqttBridgeController;
        this.rootTopic = rootTopic;
        this.client = client;
        this.brokerName = brokerName;
    }

    @Override
    public void run() {
        PersistService persistService = this.mqttBridgeController.getPersistService();
        ConcurrentHashMap<String, MqttSubscriptionData> subscriptionDataTable = this.client.getMqttSubscriptionDataTable();
        String offsetKey = new StringBuilder().append(this.brokerName).append(TOPIC_CLIENTID_SEPARATOR).append(this.rootTopic).append(TOPIC_CLIENTID_SEPARATOR).append(client.getClientId()).toString();
        try {
            long maxOffsetInQueue = getMaxOffset(this.brokerName, this.rootTopic);
            MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) this.mqttBridgeController.getMqttClientManager();
            ConcurrentHashMap<String, Long[]> consumeOffsetTable = iotClientManager.getConsumeOffsetTable();
            //获取nextOffset(从哪里开始拉取消息)
            long nextOffset = calcNextOffset(consumeOffsetTable, offsetKey, persistService);
            //compare current consumeOffset of rootTopic@clientId with maxOffset, pull message if consumeOffset < maxOffset
            log.info("nextOffset={}, maxOffsetInQueue={}", nextOffset, maxOffsetInQueue);
            while (nextOffset < maxOffsetInQueue) {
                //pull messages from enode above(brokerName), 32 messages max.
                PullMessageRequestHeader requestHeader = buildPullMessageRequestHeader(this.client.getClientId(), this.rootTopic, nextOffset);
                RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.PULL_MESSAGE, requestHeader);
                RemotingCommand response = this.mqttBridgeController.getEnodeService().pullMessageSync(this.brokerName, request);

                PullResult pullResult = processPullResponse(response, subscriptionDataTable);
                for (MessageExt messageExt : pullResult.getMsgFoundList()) {
                    String realTopic = messageExt.getProperty(MessageConst.PROPERTY_REAL_TOPIC);
                    MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_LEAST_ONCE, false, 0);
                    synchronized (client) {
                        MessageExt lastOne = pullResult.getMsgFoundList().get(pullResult.getMsgFoundList().size() - 1);
                        log.info("lastOne.Offset={}, consumeOffset={}, brokerName={}, rootTopic={}", lastOne.getQueueOffset(), consumeOffsetTable.get(offsetKey), brokerName, rootTopic);
                        if (lastOne.getQueueOffset() <= consumeOffsetTable.get(offsetKey)[1]) {
                            break;
                        }
                        log.info("messageExt.Offset={}, consumeOffset={}", messageExt.getQueueOffset(), consumeOffsetTable.get(offsetKey));
                        if (messageExt.getQueueOffset() <= consumeOffsetTable.get(offsetKey)[1]) {
                            continue;
                        }
                        log.info("consumeOffest={}, queueOffset={}, ThreadId={}", consumeOffsetTable.get(offsetKey)[0], messageExt.getQueueOffset(), Thread.currentThread().getId());
                        //push message as qos=1
                        if (!client.isConnected()) {
                            return;
                        }
                        Message message = new Message(realTopic, 1, this.brokerName, messageExt.getQueueOffset(), messageExt.getBody());
                        client.pushMessageQos1(fixedHeader, message);
                    }
                }
                nextOffset = pullResult.getNextBeginOffset();
                if (nextOffset < consumeOffsetTable.get(offsetKey)[1]) {
                    nextOffset = consumeOffsetTable.get(offsetKey)[1];
                }
            }
        } catch (Exception ex) {
            log.error("Exception was thrown when pushing messages to consumer.{}", ex);
        }
    }

    private long calcNextOffset(ConcurrentHashMap<String, Long[]> offsetTable, String offsetKey,
        PersistService persistService) {
        if (!offsetTable.containsKey(offsetKey)) {
            long persistOffset = persistService.queryConsumeOffset(offsetKey);
            Long[] offsets = new Long[2];
            offsets[0] = persistOffset;
            offsets[1] = offsets[0] - 1;
            Long[] prev = offsetTable.putIfAbsent(offsetKey, offsets);
            if (prev != null) {
                log.info("[calcNextOffset]. Offset already exist. offsetKey={}, offset={}", offsetKey, offsetTable.get(offsetKey));
            }
        }
        return offsetTable.get(offsetKey)[0];
    }

    private PullMessageRequestHeader buildPullMessageRequestHeader(String clientId, String rootTopic, long offset) {
        PullMessageRequestHeader requestHeader = new PullMessageRequestHeader();
        requestHeader.setConsumerGroup(clientId);
        requestHeader.setTopic(rootTopic);
        requestHeader.setQueueId(0);
        requestHeader.setQueueOffset(offset);
        requestHeader.setMaxMsgNums(32);
        requestHeader.setSysFlag(PullSysFlag.buildSysFlag(false, false, true, false));
        requestHeader.setCommitOffset(0L);
//        requestHeader.setSuspendTimeoutMillis(brokerSuspendMaxTimeMillis);
        requestHeader.setSubscription(rootTopic);
        requestHeader.setSubVersion(0L);
        requestHeader.setExpressionType(ExpressionType.TAG);
        return requestHeader;
    }

    private PullResult processPullResponse(final RemotingCommand response,
        ConcurrentHashMap<String, MqttSubscriptionData> subscriptionDataTable) throws MQBrokerException, RemotingCommandException {
        PullStatus pullStatus;
        PullMessageResponseHeader responseHeader =
            (PullMessageResponseHeader) response.decodeCommandCustomHeader(PullMessageResponseHeader.class);
        List<MessageExt> msgListFilterAgain = new ArrayList<>();
        switch (response.getCode()) {
            case ResponseCode.SUCCESS:
                pullStatus = PullStatus.FOUND;
                ByteBuffer byteBuffer = ByteBuffer.wrap(response.getBody());
                List<MessageExt> msgList = MessageDecoder.decodes(byteBuffer);
                //filter messages again
                for (MessageExt msg : msgList) {
                    if (msg.getProperty(MessageConst.PROPERTY_REAL_TOPIC) != null &&
                        !needSkip(msg.getProperty(MessageConst.PROPERTY_REAL_TOPIC), subscriptionDataTable)) {
                        msgListFilterAgain.add(msg);
                    }
                }
                break;
            case ResponseCode.PULL_NOT_FOUND:
                pullStatus = PullStatus.NO_NEW_MSG;
                break;
            case ResponseCode.PULL_RETRY_IMMEDIATELY:
                pullStatus = PullStatus.NO_MATCHED_MSG;
                break;
            case ResponseCode.PULL_OFFSET_MOVED:
                pullStatus = PullStatus.OFFSET_ILLEGAL;
                break;

            default:
                throw new MQBrokerException(response.getCode(), response.getRemark());
        }
        return new PullResultExt(pullStatus, responseHeader.getNextBeginOffset(), responseHeader.getMinOffset(),
            responseHeader.getMaxOffset(), msgListFilterAgain, responseHeader.getSuggestWhichBrokerId(), response.getBody());
    }

    private boolean needSkip(final String realTopic,
        ConcurrentHashMap<String, MqttSubscriptionData> subscriptionDataTable) {
        return !MqttUtil.isSubscriptionMatch(realTopic, subscriptionDataTable);

    }

    private long getMaxOffset(String enodeName,
        String topic) throws InterruptedException, RemotingTimeoutException, RemotingCommandException, RemotingSendRequestException, RemotingConnectException {
        GetMaxOffsetRequestHeader requestHeader = new GetMaxOffsetRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setQueueId(0);
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_MAX_OFFSET, requestHeader);

        return this.mqttBridgeController.getEnodeService().getMaxOffsetInQueue(enodeName, topic, 0, request);
    }
}
