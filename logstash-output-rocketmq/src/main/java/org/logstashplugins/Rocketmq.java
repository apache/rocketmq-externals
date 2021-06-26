package org.logstashplugins;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.Output;
import co.elastic.logstash.api.PluginConfigSpec;
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

// class name must match plugin name
@LogstashPlugin(name = "rocketmq")
public class Rocketmq implements Output {

    public static final PluginConfigSpec<String> NAMESRV_CONFIG =
            PluginConfigSpec.stringSetting("namesrv-addr", "", false, true);

    public static final PluginConfigSpec<String> TOPIC_CONFIG =
            PluginConfigSpec.stringSetting("topic", "", false, true);

    public static final PluginConfigSpec<String> GROUP_CONFIG =
            PluginConfigSpec.stringSetting("group-name", "logstashProducerGroup");

    private final String id;

    private String namesrvAddr;
    private String topic;
    private String groupName;

    private DefaultMQProducer producer;

    private final CountDownLatch done = new CountDownLatch(1);
    private volatile boolean stopped = false;

    // all plugins must provide a constructor that accepts id, Configuration, and Context
    public Rocketmq(final String id, final Configuration configuration, final Context context) throws MQClientException {
        this.id = id;
        this.namesrvAddr = configuration.get(NAMESRV_CONFIG);
        this.topic = configuration.get(TOPIC_CONFIG);
        this.groupName = configuration.get(GROUP_CONFIG);

        producer = new DefaultMQProducer(groupName);
        producer.setNamesrvAddr(namesrvAddr);
        producer.start();
    }

    @Override
    public void output(final Collection<Event> events) {
        Iterator<Event> z = events.iterator();
        while (z.hasNext() && !stopped) {
            Message msg = new Message(topic,z.next().toString().getBytes());
            try {
                producer.send(msg, new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        System.out.printf("%-10d OK %n", sendResult.getMsgId());
                    }
                    @Override
                    public void onException(Throwable e) {
                        System.out.printf("%-10d Exception %n", e);
                        e.printStackTrace();
                    }
                });
            } catch (MQClientException | RemotingException | InterruptedException e) {
                e.printStackTrace();
            }
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
        return Arrays.asList(NAMESRV_CONFIG,TOPIC_CONFIG,GROUP_CONFIG);
    }

    @Override
    public String getId() {
        return id;
    }
}
