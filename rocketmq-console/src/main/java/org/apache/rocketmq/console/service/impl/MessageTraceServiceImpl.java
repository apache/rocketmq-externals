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

package org.apache.rocketmq.console.service.impl;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import com.google.common.base.Throwables;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.trace.TraceType;
import org.apache.rocketmq.common.Pair;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.topic.TopicValidator;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.model.MessageTraceView;
import org.apache.rocketmq.console.model.trace.ProducerNode;
import org.apache.rocketmq.console.model.trace.MessageTraceGraph;
import org.apache.rocketmq.console.model.trace.SubscriptionNode;
import org.apache.rocketmq.console.model.trace.TraceNode;
import org.apache.rocketmq.console.service.MessageTraceService;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class MessageTraceServiceImpl implements MessageTraceService {

    private Logger logger = LoggerFactory.getLogger(MessageTraceServiceImpl.class);

    private final static int QUERY_MESSAGE_MAX_NUM = 64;
    @Resource
    private MQAdminExt mqAdminExt;

    @Resource
    private RMQConfigure rmqConfigure;

    @Override
    public List<MessageTraceView> queryMessageTraceKey(String key) {
        String queryTopic = rmqConfigure.getMsgTrackTopicName();
        if (StringUtils.isEmpty(queryTopic)) {
            queryTopic = TopicValidator.RMQ_SYS_TRACE_TOPIC;
        }
        logger.info("query data topic name is:{}", queryTopic);
        return queryMessageTraceByTopicAndKey(queryTopic, key);
    }

    @Override
    public List<MessageTraceView> queryMessageTraceByTopicAndKey(String topic, String key) {
        try {
            List<MessageTraceView> messageTraceViews = new ArrayList<MessageTraceView>();
            List<MessageExt> messageTraceList = mqAdminExt.queryMessage(topic, key, QUERY_MESSAGE_MAX_NUM, 0, System.currentTimeMillis()).getMessageList();
            for (MessageExt messageExt : messageTraceList) {
                List<MessageTraceView> messageTraceView = MessageTraceView.decodeFromTraceTransData(key, messageExt);
                messageTraceViews.addAll(messageTraceView);
            }
            return messageTraceViews;
        } catch (Exception err) {
            throw Throwables.propagate(err);
        }
    }

    @Override
    public MessageTraceGraph queryMessageTraceGraph(String key) {
        List<MessageTraceView> messageTraceViews = queryMessageTraceKey(key);
        return buildMessageTraceGraph(messageTraceViews);
    }

    private MessageTraceGraph buildMessageTraceGraph(List<MessageTraceView> messageTraceViews) {
        MessageTraceGraph messageTraceGraph = new MessageTraceGraph();
        messageTraceGraph.setMessageTraceViews(messageTraceViews);
        if (CollectionUtils.isEmpty(messageTraceViews)) {
            return messageTraceGraph;
        }
        ProducerNode producerNode = null;
        List<TraceNode> transactionNodeList = new ArrayList<>();
        Map<String, Pair<MessageTraceView, MessageTraceView>> requestIdTracePairMap = Maps.newHashMap();
        for (MessageTraceView messageTraceView : messageTraceViews) {
            switch (TraceType.valueOf(messageTraceView.getTraceType())) {
                case Pub:
                    producerNode = buildMessageRoot(messageTraceView);
                    break;
                case EndTransaction:
                    transactionNodeList.add(buildTransactionNode(messageTraceView));
                    break;
                case SubBefore:
                case SubAfter:
                    putIntoMessageTraceViewGroupMap(messageTraceView, requestIdTracePairMap);
                    break;
                default:
                    break;
            }
        }
        if (producerNode != null) {
            producerNode.setTransactionNodeList(sortTraceNodeListByBeginTimestamp(transactionNodeList));
        }
        messageTraceGraph.setProducerNode(producerNode);
        messageTraceGraph.setSubscriptionNodeList(buildSubscriptionNodeList(requestIdTracePairMap));
        return messageTraceGraph;
    }

    private TraceNode buildTransactionNode(MessageTraceView messageTraceView) {
        return buildTraceNode(messageTraceView);
    }

    private List<SubscriptionNode> buildSubscriptionNodeList(
        Map<String, Pair<MessageTraceView, MessageTraceView>> requestIdTracePairMap) {
        Map<String, List<TraceNode>> subscriptionTraceNodeMap = Maps.newHashMap();
        for (Pair<MessageTraceView, MessageTraceView> traceNodePair : requestIdTracePairMap.values()) {
            MessageTraceView subBeforeTrace = traceNodePair.getObject1();
            MessageTraceView subAfterTrace = traceNodePair.getObject2();
            List<TraceNode> traceNodeList = subscriptionTraceNodeMap.computeIfAbsent(subBeforeTrace.getGroupName(),
                (o) -> Lists.newArrayList());
            TraceNode consumeNode = new TraceNode();
            consumeNode.setRequestId(subBeforeTrace.getRequestId());
            consumeNode.setStoreHost(subBeforeTrace.getStoreHost());
            consumeNode.setClientHost(subBeforeTrace.getClientHost());
            consumeNode.setRetryTimes(subBeforeTrace.getRetryTimes());
            consumeNode.setBeginTimeStamp(subBeforeTrace.getTimeStamp());
            consumeNode.setCostTime(subAfterTrace.getCostTime());
            consumeNode.setEndTimeStamp(subBeforeTrace.getTimeStamp() + subAfterTrace.getCostTime());
            consumeNode.setStatus(subAfterTrace.getStatus());
            traceNodeList.add(consumeNode);
        }
        return subscriptionTraceNodeMap.entrySet().stream()
            .map((Function<Map.Entry<String, List<TraceNode>>, SubscriptionNode>) subscriptionEntry -> {
                List<TraceNode> traceNodeList = subscriptionEntry.getValue();
                SubscriptionNode subscriptionNode = new SubscriptionNode();
                subscriptionNode.setSubscriptionGroup(subscriptionEntry.getKey());
                subscriptionNode.setConsumeNodeList(sortTraceNodeListByBeginTimestamp(traceNodeList));
                return subscriptionNode;
            }).collect(Collectors.toList());
    }

    private void putIntoMessageTraceViewGroupMap(MessageTraceView messageTraceView,
                                                 Map<String, Pair<MessageTraceView, MessageTraceView>> messageTraceViewGroupMap) {
        Pair<MessageTraceView, MessageTraceView> messageTracePair = messageTraceViewGroupMap
            .computeIfAbsent(messageTraceView.getRequestId(), (o) -> new Pair<>(null, null));
        switch (TraceType.valueOf(messageTraceView.getTraceType())) {
            case SubBefore:
                messageTracePair.setObject1(messageTraceView);
                break;
            case SubAfter:
                messageTracePair.setObject2(messageTraceView);
                break;
            default:
                break;
        }
    }

    private ProducerNode buildMessageRoot(MessageTraceView messageTraceView) {
        ProducerNode root = new ProducerNode();
        BeanUtils.copyProperties(messageTraceView, root);
        root.setTraceNode(buildTraceNode(messageTraceView));
        return root;
    }

    private TraceNode buildTraceNode(MessageTraceView messageTraceView) {
        TraceNode traceNode = new TraceNode();
        BeanUtils.copyProperties(messageTraceView, traceNode);
        traceNode.setBeginTimeStamp(messageTraceView.getTimeStamp());
        traceNode.setEndTimeStamp(messageTraceView.getTimeStamp() + messageTraceView.getCostTime());
        return traceNode;
    }

    private List<TraceNode> sortTraceNodeListByBeginTimestamp(List<TraceNode> traceNodeList) {
        traceNodeList.sort((o1, o2) -> -Long.compare(o1.getBeginTimeStamp(), o2.getBeginTimeStamp()));
        return traceNodeList;
    }
}
