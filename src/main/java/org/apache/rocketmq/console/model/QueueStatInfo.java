package org.apache.rocketmq.console.model;

import com.alibaba.rocketmq.common.admin.OffsetWrapper;
import com.alibaba.rocketmq.common.message.MessageQueue;
import org.springframework.beans.BeanUtils;

/**
 * Created by tangjie
 * 2016/11/22
 * styletang.me@gmail.com
 */
public class QueueStatInfo {
    private String brokerName;
    private int queueId;
    private long brokerOffset;
    private long consumerOffset;
    // 消费的最后一条消息对应的时间戳
    private long lastTimestamp;
    public static QueueStatInfo fromOffsetTableEntry(MessageQueue key, OffsetWrapper value){
        QueueStatInfo queueStatInfo = new QueueStatInfo();
        BeanUtils.copyProperties(key,queueStatInfo);
        BeanUtils.copyProperties(value,queueStatInfo);
        return queueStatInfo;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public int getQueueId() {
        return queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    public long getBrokerOffset() {
        return brokerOffset;
    }

    public void setBrokerOffset(long brokerOffset) {
        this.brokerOffset = brokerOffset;
    }

    public long getConsumerOffset() {
        return consumerOffset;
    }

    public void setConsumerOffset(long consumerOffset) {
        this.consumerOffset = consumerOffset;
    }

    public long getLastTimestamp() {
        return lastTimestamp;
    }

    public void setLastTimestamp(long lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }
}
