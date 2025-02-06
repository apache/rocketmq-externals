package org.logstashplugins;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.Output;
import co.elastic.logstash.api.PluginConfigSpec;
import org.apache.log4j.Logger;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

/**
 * @Author: ZHOU Jian
 * @Date: 2021-05
 */
@LogstashPlugin(name = "rocketmq")
public class RocketMQ implements Output {
    private static Logger logger = Logger.getLogger(RocketMQ.class);

    private final static String SYNC = "sync";
    private final static String ASYNC = "async";
    private final static String ONEWAY = "oneway";

    public static final PluginConfigSpec<String> GROUP_CONFIG =
            PluginConfigSpec.stringSetting("group-name", "defaultProducerGroup");

    public static final PluginConfigSpec<String> NAMESRV_CONFIG =
            PluginConfigSpec.stringSetting("namesrv-addr", "", false, true);

    public static final PluginConfigSpec<String> TOPIC_CONFIG =
            PluginConfigSpec.stringSetting("topic", "", false, true);

    public static final PluginConfigSpec<String> TAG_CONFIG =
            PluginConfigSpec.stringSetting("tag", "defaultTag");

    public static final PluginConfigSpec<String> SEND_CONFIG =
            PluginConfigSpec.stringSetting("send", ONEWAY);

    public static final PluginConfigSpec<String> TIMEOUT_CONFIG =
            PluginConfigSpec.stringSetting("send-timeout", "3000");

    public static final PluginConfigSpec<String> RETRY_CONFIG =
            PluginConfigSpec.stringSetting("retry-times", "2");

    private final String id;
    private String groupName;
    private String namesrvAddr;
    private String topic;
    private String tag;
    private String send;
    private Long sendTimeout;
    private Integer retryTimes;

    private DefaultMQProducer producer;

    private final CountDownLatch done = new CountDownLatch(1);
    private volatile boolean stopped = false;

    // all plugins must provide a constructor that accepts id, Configuration, and Context
    public RocketMQ(final String id, final Configuration configuration, final Context context) throws MQClientException {
        this.id = id;
        this.groupName = configuration.get(GROUP_CONFIG);
        this.namesrvAddr = configuration.get(NAMESRV_CONFIG);
        this.topic = configuration.get(TOPIC_CONFIG);
        this.tag = configuration.get(TAG_CONFIG);
        this.send = configuration.get(SEND_CONFIG);
        this.sendTimeout = Long.parseLong(configuration.get(TIMEOUT_CONFIG));
        this.retryTimes = Integer.parseInt(configuration.get(RETRY_CONFIG));

        producer = new DefaultMQProducer(groupName);
        producer.setNamesrvAddr(namesrvAddr);
        producer.setRetryTimesWhenSendFailed(retryTimes);
        producer.setRetryTimesWhenSendAsyncFailed(retryTimes);
        producer.start();
    }

    @Override
    public void output(final Collection<Event> events) {
        Iterator<Event> z = events.iterator();
        switch (send){
            case ONEWAY:
                while (z.hasNext() && !stopped) {
                    Message message = new Message(topic, tag, z.next().toString().getBytes());
                    try {
                        producer.sendOneway(message);
                        logger.info("send one-way message");
                    } catch (MQClientException | RemotingException | InterruptedException e) {
                        logger.error("send one-way message error", e);
                    }
                }
                break;
            case SYNC:
                while (z.hasNext() && !stopped) {
                    Message message = new Message(topic, tag, z.next().toString().getBytes());
                    try {
                        SendResult sendResult = producer.send(message, sendTimeout);
                        logger.info("send sync message: " + sendResult.toString());

                    } catch (MQClientException | RemotingException | MQBrokerException | InterruptedException e) {
                        logger.error("send sync message error", e);
                    }
                }
                break;
            case ASYNC:
                while (z.hasNext() && !stopped) {
                    Message message = new Message(topic, tag, z.next().toString().getBytes());
                    try {
                        producer.send(message, new SendCallback() {
                            @Override
                            public void onSuccess(SendResult sendResult) {
                                logger.info("send async message: " + sendResult.toString());
                            }
                            @Override
                            public void onException(Throwable e) {
                                logger.error("send async message error", e);
                            }
                        }, sendTimeout);
                    } catch (MQClientException | RemotingException | InterruptedException e) {
                        logger.error("send async message error", e);
                    }
                }
                break;
        }
    }

    @Override
    public void stop() {
        stopped = true;
        done.countDown();
        producer.shutdown();
    }

    @Override
    public void awaitStop() throws InterruptedException {
        done.await();
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        // should return a list of all configuration options for this plugin
        return Arrays.asList(GROUP_CONFIG, NAMESRV_CONFIG, TOPIC_CONFIG, TAG_CONFIG,
                SEND_CONFIG, TIMEOUT_CONFIG, RETRY_CONFIG);
    }

    @Override
    public String getId() {
        return id;
    }
}
