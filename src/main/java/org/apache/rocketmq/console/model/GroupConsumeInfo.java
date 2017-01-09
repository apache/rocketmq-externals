package org.apache.rocketmq.console.model;

import com.alibaba.rocketmq.common.protocol.heartbeat.ConsumeType;
import com.alibaba.rocketmq.common.protocol.heartbeat.MessageModel;

/**
 * Created by tangjie
 * 2016/11/22
 * styletang.me@gmail.com
 */
public class GroupConsumeInfo implements Comparable<GroupConsumeInfo> {
    private String group;
    private String version;
    private int count;
    private ConsumeType consumeType;
    private MessageModel messageModel;
    private int consumeTps;
    private long diffTotal;


    public String getGroup() {
        return group;
    }


    public void setGroup(String group) {
        this.group = group;
    }


    public int getCount() {
        return count;
    }


    public void setCount(int count) {
        this.count = count;
    }


    public ConsumeType getConsumeType() {
        return consumeType;
    }


    public void setConsumeType(ConsumeType consumeType) {
        this.consumeType = consumeType;
    }


    public MessageModel getMessageModel() {
        return messageModel;
    }


    public void setMessageModel(MessageModel messageModel) {
        this.messageModel = messageModel;
    }


    public long getDiffTotal() {
        return diffTotal;
    }


    public void setDiffTotal(long diffTotal) {
        this.diffTotal = diffTotal;
    }


    @Override
    public int compareTo(GroupConsumeInfo o) {
        if (this.count != o.count) {
            return o.count - this.count;
        }

        return (int) (o.diffTotal - diffTotal);
    }


    public int getConsumeTps() {
        return consumeTps;
    }


    public void setConsumeTps(int consumeTps) {
        this.consumeTps = consumeTps;
    }


    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
