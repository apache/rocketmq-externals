package org.logstashplugins;

import co.elastic.logstash.api.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

// class name must match plugin name
@LogstashPlugin(name = "logstash_output_rocketmq")
public class LogstashOutputRocketmq implements Output {

    public static final PluginConfigSpec<String> PRODUCER_GROUP =
            PluginConfigSpec.stringSetting("producerGroup", "ProducerGroupName", false, false);
    public static final PluginConfigSpec<String> TOPIC =
            PluginConfigSpec.stringSetting("topic", "", false, true);
    public static final PluginConfigSpec<String> NAMESRV_Addr =
            PluginConfigSpec.stringSetting("nameSrvAddr", "", false, true);
    public static final PluginConfigSpec<String> TAG =
            PluginConfigSpec.stringSetting("tag", "from_logstash", false, false);
    public static final PluginConfigSpec<String> SEND_WAY =
            PluginConfigSpec.stringSetting("send_way", "one_way", false, false);


    private final String id;
    private final String topic;
    private final String tag;
    private final String send_way;
    private final CountDownLatch done = new CountDownLatch(1);
    private volatile boolean stopped = false;
    private DefaultMQProducer producer;
    private final Log logger = LogFactory.getLog(LogstashOutputRocketmq.class);
    // all plugins must provide a constructor that accepts id, Configuration, and Context

    public LogstashOutputRocketmq(final String id, final Configuration config, final Context context) {
        // constructors should validate configuration options
        this.id = id;
        String producerGroup = config.get(PRODUCER_GROUP);
        topic = config.get(TOPIC);
        String namSrvAddr = config.get(NAMESRV_Addr);
        tag = config.get(TAG);
        send_way = config.get(SEND_WAY);
        String[] send_way_list = {"one_way", "sync", "async"};
        if (!Arrays.asList(send_way_list).contains(send_way)) {
            System.out.println("error setting in send_way\n");
            System.out.println("must in one_way,sync,async");
            System.exit(-9);
        }
        try {
            producer = new DefaultMQProducer(producerGroup);
            producer.setNamesrvAddr(namSrvAddr);
            producer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void send(Message msg) throws Exception {
        if (send_way.equals("one_way")) {
            producer.sendOneway(msg);
            logger.info("send one way msg " + msg);
        } else if (send_way.equals("sync")) {
            SendResult sendResult = producer.send(msg);
            logger.info(String.format("send sync msg %s ,result = %s", msg.toString(), sendResult.toString()));
        } else if (send_way.equals("async")) {
            producer.send(msg, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    logger.info(String.format("send sync msg ,result = %s", sendResult.toString()));
                }

                @Override
                public void onException(Throwable e) {
                    logger.error("send asyn msg fail , " + e);
                }
            });
        }


    }

    @Override
    public void output(final Collection<Event> events) {
        Iterator<Event> z = events.iterator();
        try {
            while (z.hasNext() && !stopped) {
                Message msg = new Message(topic, tag, z.next().toString().getBytes(RemotingHelper.DEFAULT_CHARSET));
                send(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        return Arrays.asList(PRODUCER_GROUP, TOPIC, NAMESRV_Addr, TAG, SEND_WAY);
    }

    @Override
    public String getId() {
        return id;
    }
}
