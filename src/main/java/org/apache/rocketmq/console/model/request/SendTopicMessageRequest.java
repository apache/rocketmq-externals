package org.apache.rocketmq.console.model.request;

/**
 * Created by tangjie
 * 2016/11/29
 * styletang.me@gmail.com
 */
public class SendTopicMessageRequest {
    private String topic;
    private String key;
    private String tag;
    private String messageBody;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }
}
