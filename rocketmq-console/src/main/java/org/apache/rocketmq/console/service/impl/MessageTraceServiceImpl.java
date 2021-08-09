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
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.model.MessageTraceView;
import org.apache.rocketmq.console.model.trace.ProducerNode;
import org.apache.rocketmq.console.model.trace.MessageTraceGraph;
import org.apache.rocketmq.console.model.trace.SubscriptionNode;
import org.apache.rocketmq.console.model.trace.TraceNode;
import org.apache.rocketmq.console.model.trace.MessageTraceStatusEnum;
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

    private final static String UNKNOWN_GROUP_NAME = "%UNKNOWN_GROUP%";
    private final static int MESSAGE_TRACE_MISSING_VALUE = -1;
    @Resource
    private MQAdminExt mqAdminExt;

    @Resource
    private RMQConfigure configure;

    @Override
    public List<MessageTraceView> queryMessageTraceKey(String key) {
        String queryTopic = configure.getMsgTrackTopicNameOrDefault();
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
        TraceNode transactionNode = buildTraceNode(messageTraceView);
        transactionNode.setCostTime(MESSAGE_TRACE_MISSING_VALUE);
        return transactionNode;
    }

    private List<SubscriptionNode> buildSubscriptionNodeList(
        Map<String, Pair<MessageTraceView, MessageTraceView>> requestIdTracePairMap) {
        Map<String, List<TraceNode>> subscriptionTraceNodeMap = Maps.newHashMap();
        for (Pair<MessageTraceView, MessageTraceView> traceNodePair : requestIdTracePairMap.values()) {
            List<TraceNode> traceNodeList = subscriptionTraceNodeMap
                .computeIfAbsent(buildGroupName(traceNodePair), (o) -> Lists.newArrayList());
            traceNodeList.add(buildConsumeMessageTraceNode(traceNodePair));
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

    private <E> E getTraceValue(Pair<MessageTraceView, MessageTraceView> traceNodePair, Function<MessageTraceView, E> function) {
        if (traceNodePair.getObject1() != null) {
            return function.apply(traceNodePair.getObject1());
        }
        return function.apply(traceNodePair.getObject2());
    }

    private String buildGroupName(Pair<MessageTraceView, MessageTraceView> traceNodePair) {
        String groupName = getTraceValue(traceNodePair, MessageTraceView::getGroupName);
        if (StringUtils.isNoneBlank(groupName)) {
            return groupName;
        }
        return UNKNOWN_GROUP_NAME;
    }

    private TraceNode buildConsumeMessageTraceNode(Pair<MessageTraceView, MessageTraceView> pair) {
        MessageTraceView subBeforeTrace = pair.getObject1();
        MessageTraceView subAfterTrace = pair.getObject2();
        TraceNode consumeNode = new TraceNode();
        consumeNode.setRequestId(getTraceValue(pair, MessageTraceView::getRequestId));
        consumeNode.setStoreHost(getTraceValue(pair, MessageTraceView::getStoreHost));
        consumeNode.setClientHost(getTraceValue(pair, MessageTraceView::getClientHost));
        if (subBeforeTrace != null) {
            consumeNode.setRetryTimes(subBeforeTrace.getRetryTimes());
            consumeNode.setBeginTimestamp(subBeforeTrace.getTimeStamp());
        } else {
            consumeNode.setRetryTimes(MESSAGE_TRACE_MISSING_VALUE);
            consumeNode.setBeginTimestamp(MESSAGE_TRACE_MISSING_VALUE);
        }
        if (subAfterTrace != null) {
            consumeNode.setCostTime(subAfterTrace.getCostTime());
            consumeNode.setStatus(subAfterTrace.getStatus());
            if (subAfterTrace.getTimeStamp() > 0) {
                consumeNode.setEndTimestamp(subAfterTrace.getTimeStamp());
            } else {
                if (subBeforeTrace != null) {
                    if (subAfterTrace.getCostTime() >= 0) {
                        consumeNode.setEndTimestamp(subBeforeTrace.getTimeStamp() + subAfterTrace.getCostTime());
                    } else {
                        consumeNode.setEndTimestamp(subBeforeTrace.getTimeStamp());
                    }
                } else {
                    consumeNode.setEndTimestamp(MESSAGE_TRACE_MISSING_VALUE);
                }
            }
        } else {
            consumeNode.setCostTime(MESSAGE_TRACE_MISSING_VALUE);
            consumeNode.setEndTimestamp(MESSAGE_TRACE_MISSING_VALUE);
            consumeNode.setStatus(MessageTraceStatusEnum.UNKNOWN.getStatus());
        }
        return consumeNode;
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
        traceNode.setBeginTimestamp(messageTraceView.getTimeStamp());
        traceNode.setEndTimestamp(messageTraceView.getTimeStamp() + messageTraceView.getCostTime());
        return traceNode;
    }

    private List<TraceNode> sortTraceNodeListByBeginTimestamp(List<TraceNode> traceNodeList) {
        traceNodeList.sort((o1, o2) -> -Long.compare(o1.getBeginTimestamp(), o2.getBeginTimestamp()));
        return traceNodeList;
    }
}
