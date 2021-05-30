package org.logstashplugins.utils;

import co.elastic.logstash.api.Configuration;
import org.apache.rocketmq.client.producer.DefaultMQProducer;

public class ProducerByConfig {


    private final String nameSrvAddr;
    private final String producerGroup;
    private final int defaultTopicQueueNums;
    public final int sendMsgTimeout;
    private final int compressMsgBodyOverHowmuch;
    private final int retryTimesWhenSendFailed;
    private final int retryTimesWhenSendAsyncFailed;
    private final boolean retryAnotherBrokerWhenNotStoreOK;
    private final int maxMessageSize;

    public ProducerByConfig(Configuration config) {
        this.nameSrvAddr = config.get(PluginConfigParams.NAMESRV_CONFIG);
        this.producerGroup = config.get(PluginConfigParams.PRODUCER_GROUP_CONFIG);
        this.defaultTopicQueueNums = Integer.parseInt(config.get(PluginConfigParams.DEFAULT_TOPIC_QUEUE_NUMS_CONFIG));
        this.sendMsgTimeout = Integer.parseInt(config.get(PluginConfigParams.SEND_MSG_TIMEOUT_CONFIG));
        this.compressMsgBodyOverHowmuch = Integer.parseInt(config.get(PluginConfigParams.COMPRESS_MSG_BODY_OVER_HOWMUCH_GONFIG));
        this.retryTimesWhenSendAsyncFailed = Integer.parseInt(config.get(PluginConfigParams.RETRY_TIMES_WHEN_SEND_ASYHCFAILD_CONFIG));
        this.retryTimesWhenSendFailed = Integer.parseInt(config.get(PluginConfigParams.RETRY_TIMES_WHEN_SEND_FAILD_CONFIG));
        this.retryAnotherBrokerWhenNotStoreOK = config.get(PluginConfigParams.RETRY_ANOTHER_BROKER_WHEN_NOT_STRRE_OK);
        this.maxMessageSize = Integer.parseInt(config.get(PluginConfigParams.MAX_MESSAGE_SIZE));
    }

    public DefaultMQProducer getMQProducer() {
        DefaultMQProducer producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(nameSrvAddr);
        producer.setSendMsgTimeout(sendMsgTimeout);
        producer.setDefaultTopicQueueNums(defaultTopicQueueNums);
        producer.setRetryTimesWhenSendAsyncFailed(retryTimesWhenSendAsyncFailed);
        producer.setMaxMessageSize(maxMessageSize);
        producer.setRetryTimesWhenSendFailed(retryTimesWhenSendFailed);
        producer.setCompressMsgBodyOverHowmuch(compressMsgBodyOverHowmuch);
        producer.setRetryAnotherBrokerWhenNotStoreOK(retryAnotherBrokerWhenNotStoreOK);
        return producer;
    }


}
