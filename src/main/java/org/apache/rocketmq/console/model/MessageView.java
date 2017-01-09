package org.apache.rocketmq.console.model;

import com.alibaba.rocketmq.common.message.MessageExt;
import com.google.common.base.Charsets;
import org.springframework.beans.BeanUtils;

import java.net.SocketAddress;
import java.util.Map;

/**
 * Created by tangjie
 * 2016/11/25
 * styletang.me@gmail.com
 */
public class MessageView {

    /**from MessageExt**/
    // 队列ID <PUT>
    private int queueId;
    // 存储记录大小
    private int storeSize;
    // 队列偏移量
    private long queueOffset;
    // 消息标志位 <PUT>
    private int sysFlag;
    // 消息在客户端创建时间戳 <PUT>
    private long bornTimestamp;
    // 消息来自哪里 <PUT>
    private SocketAddress bornHost;
    // 消息在服务器存储时间戳
    private long storeTimestamp;
    // 消息存储在哪个服务器 <PUT>
    private SocketAddress storeHost;
    // 消息ID
    private String msgId;
    // 消息对应的Commit Log Offset
    private long commitLogOffset;
    // 消息体CRC
    private int bodyCRC;
    // 当前消息被某个订阅组重新消费了几次（订阅组之间独立计数）
    private int reconsumeTimes;
    private long preparedTransactionOffset;
    /**from MessageExt**/

    /**from Message**/
    private String topic;
    private int flag;
    private Map<String, String> properties;
    private String messageBody; // body
    /**from Message**/

    public static MessageView fromMessageExt(MessageExt messageExt) {
        MessageView messageView = new MessageView();
        BeanUtils.copyProperties(messageExt, messageView);
        if (messageExt.getBody() != null) {
            messageView.setMessageBody(new String(messageExt.getBody(), Charsets.UTF_8));
        }
        return messageView;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public int getQueueId() {
        return queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    public int getStoreSize() {
        return storeSize;
    }

    public void setStoreSize(int storeSize) {
        this.storeSize = storeSize;
    }

    public long getQueueOffset() {
        return queueOffset;
    }

    public void setQueueOffset(long queueOffset) {
        this.queueOffset = queueOffset;
    }

    public int getSysFlag() {
        return sysFlag;
    }

    public void setSysFlag(int sysFlag) {
        this.sysFlag = sysFlag;
    }

    public long getBornTimestamp() {
        return bornTimestamp;
    }

    public void setBornTimestamp(long bornTimestamp) {
        this.bornTimestamp = bornTimestamp;
    }

    public SocketAddress getBornHost() {
        return bornHost;
    }

    public void setBornHost(SocketAddress bornHost) {
        this.bornHost = bornHost;
    }

    public long getStoreTimestamp() {
        return storeTimestamp;
    }

    public void setStoreTimestamp(long storeTimestamp) {
        this.storeTimestamp = storeTimestamp;
    }

    public SocketAddress getStoreHost() {
        return storeHost;
    }

    public void setStoreHost(SocketAddress storeHost) {
        this.storeHost = storeHost;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public long getCommitLogOffset() {
        return commitLogOffset;
    }

    public void setCommitLogOffset(long commitLogOffset) {
        this.commitLogOffset = commitLogOffset;
    }

    public int getBodyCRC() {
        return bodyCRC;
    }

    public void setBodyCRC(int bodyCRC) {
        this.bodyCRC = bodyCRC;
    }

    public int getReconsumeTimes() {
        return reconsumeTimes;
    }

    public void setReconsumeTimes(int reconsumeTimes) {
        this.reconsumeTimes = reconsumeTimes;
    }

    public long getPreparedTransactionOffset() {
        return preparedTransactionOffset;
    }

    public void setPreparedTransactionOffset(long preparedTransactionOffset) {
        this.preparedTransactionOffset = preparedTransactionOffset;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }
}
