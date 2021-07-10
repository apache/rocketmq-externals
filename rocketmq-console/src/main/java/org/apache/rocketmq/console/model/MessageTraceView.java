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

package org.apache.rocketmq.console.model;

import com.google.common.base.Charsets;
import org.apache.rocketmq.client.trace.TraceBean;
import org.apache.rocketmq.client.trace.TraceContext;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.console.util.MsgTraceDecodeUtil;

import java.util.ArrayList;
import java.util.List;

public class MessageTraceView {
    private String requestId;
    private String msgId;
    private String tags;
    private String keys;
    private String storeHost;
    private String clientHost;
    private int costTime;
    private String msgType;
    private String offSetMsgId;
    private long timeStamp;
    private String topic;
    private String groupName;
    private int retryTimes;
    private String status;
    private String transactionState;
    private String transactionId;
    private boolean fromTransactionCheck;
    private String traceType;

    public MessageTraceView() {
    }

    public static List<MessageTraceView> decodeFromTraceTransData(String key, MessageExt messageExt) {
        List<MessageTraceView> messageTraceViewList = new ArrayList<MessageTraceView>();
        String messageBody = new String(messageExt.getBody(), Charsets.UTF_8);
        if (messageBody == null || messageBody.length() <= 0) {
            return messageTraceViewList;
        }

        List<TraceContext> traceContextList = MsgTraceDecodeUtil.decoderFromTraceDataString(messageBody);
        for (TraceContext context : traceContextList) {
            MessageTraceView messageTraceView = new MessageTraceView();
            TraceBean traceBean = context.getTraceBeans().get(0);
            if (!traceBean.getMsgId().equals(key)) {
                continue;
            }
            messageTraceView.setCostTime(context.getCostTime());
            messageTraceView.setGroupName(context.getGroupName());
            if (context.isSuccess()) {
                messageTraceView.setStatus("success");
            } else {
                messageTraceView.setStatus("failed");
            }
            messageTraceView.setKeys(traceBean.getKeys());
            messageTraceView.setMsgId(traceBean.getMsgId());
            messageTraceView.setTags(traceBean.getTags());
            messageTraceView.setTopic(traceBean.getTopic());
            messageTraceView.setMsgType(traceBean.getMsgType() == null ? null : traceBean.getMsgType().name());
            messageTraceView.setOffSetMsgId(traceBean.getOffsetMsgId());
            messageTraceView.setTimeStamp(context.getTimeStamp());
            messageTraceView.setStoreHost(traceBean.getStoreHost());
            messageTraceView.setClientHost(messageExt.getBornHostString());
            messageTraceView.setRequestId(context.getRequestId());
            messageTraceView.setRetryTimes(traceBean.getRetryTimes());
            messageTraceView.setTransactionState(traceBean.getTransactionState() == null ? null : traceBean.getTransactionState().name());
            messageTraceView.setTransactionId(traceBean.getTransactionId());
            messageTraceView.setFromTransactionCheck(traceBean.isFromTransactionCheck());
            messageTraceView.setTraceType(context.getTraceType() == null ? null : context.getTraceType().name());
            messageTraceViewList.add(messageTraceView);
        }
        return messageTraceViewList;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getKeys() {
        return keys;
    }

    public void setKeys(String keys) {
        this.keys = keys;
    }

    public String getStoreHost() {
        return storeHost;
    }

    public void setStoreHost(String storeHost) {
        this.storeHost = storeHost;
    }

    public int getCostTime() {
        return costTime;
    }

    public void setCostTime(int costTime) {
        this.costTime = costTime;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getOffSetMsgId() {
        return offSetMsgId;
    }

    public void setOffSetMsgId(String offSetMsgId) {
        this.offSetMsgId = offSetMsgId;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getClientHost() {
        return clientHost;
    }

    public void setClientHost(String clientHost) {
        this.clientHost = clientHost;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public String getTransactionState() {
        return transactionState;
    }

    public void setTransactionState(String transactionState) {
        this.transactionState = transactionState;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public boolean isFromTransactionCheck() {
        return fromTransactionCheck;
    }

    public void setFromTransactionCheck(boolean fromTransactionCheck) {
        this.fromTransactionCheck = fromTransactionCheck;
    }

    public String getTraceType() {
        return traceType;
    }

    public void setTraceType(String traceType) {
        this.traceType = traceType;
    }
}
