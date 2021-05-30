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
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.logstashplugins.utils.PluginConfigParams;
import org.logstashplugins.utils.ProducerByConfig;
import org.logstashplugins.utils.sendcallback.SendCallBackImpl1;
import org.logstashplugins.utils.sendcallback.SendCallBackImpl2;
import org.logstashplugins.utils.sendcallback.SendEmail;

import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.CountDownLatch;

// class name must match plugin name
@LogstashPlugin(name = "rocketmq")
public class Rocketmq implements Output {

    public static final PluginConfigSpec<String> PREFIX_CONFIG =
            PluginConfigSpec.stringSetting("prefix", "");
    private final String id;
    private final String prefix;
    private final CountDownLatch done = new CountDownLatch(1);
    private volatile boolean stopped = false;
    public Configuration config;
    private final DefaultMQProducer producer;
    private final String topic;
    private final String key;
    private final String tag;
    private final int flag;
    private final int sendMode;
    private List<Message> batch;
    private final int sendMsgTimeout;
    private SendCallback sendCallback;
    private final boolean email;
    private SendEmail sendEmail;

    // all plugins must provide a constructor that accepts id, Configuration, and Context
    public Rocketmq(final String id, final Configuration configuration, final Context context) {
        this(id, configuration, context, System.out);
    }

    Rocketmq(final String id, final Configuration config, final Context context, OutputStream targetStream) {
        // constructors should validate configuration options
        this.id = id;
        this.prefix = config.get(PREFIX_CONFIG);
        this.config = config;

        ProducerByConfig producerByConfig = new ProducerByConfig(config);
        producer = producerByConfig.getMQProducer();
        try {
            producer.start();
        } catch (MQClientException e) {
            e.printStackTrace();
        }
        topic = config.get(PluginConfigParams.TOPIC_CONFIG);
        flag = Integer.parseInt(config.get(PluginConfigParams.FLAG_CONFIG));
        tag = config.get(PluginConfigParams.TAG_CONFIG);
        key = config.get(PluginConfigParams.KEY_CONFIG);
        email = config.get(PluginConfigParams.SEND_EMAIL_FAILED_CONFIG);

        if (email) {
            sendEmail = new SendEmail(config);
        }
        sendMode = PluginConfigParams.transeferMode(config.get(PluginConfigParams.SEND_MODE));
        if (sendMode == 1) {
            sendCallback = email ? new SendCallBackImpl1(sendEmail) : new SendCallBackImpl2();
        }
        if (sendMode == 3) {
            batch = new ArrayList<>();
        }
        sendMsgTimeout = Integer.parseInt(config.get(PluginConfigParams.SEND_MSG_TIMEOUT_CONFIG));
    }

    @Override
    public void output(final Collection<Event> events) {
        Iterator<Event> z = events.iterator();
        while (z.hasNext() && !stopped) {
            String s = prefix + z.next();

            Message msg = new Message();
            msg.setTopic(topic);
            msg.setBody(s.getBytes());
            if (flag != 0) {
                msg.setFlag(flag);
            }
            msg.setKeys(key);
            msg.setTags(tag);

            SendResult sendResult;
            try {
                switch (sendMode) {
                    case 0:
                        sendResult = producer.send(msg, sendMsgTimeout);
                        processSendResult(sendResult, msg);
                        break;
                    case 1:
                        producer.send(msg, sendCallback, sendMsgTimeout);
                        break;
                    case 2:
                        producer.sendOneway(msg);
                        break;
                    case 3:
                        batch.add(msg);
                        if (!z.hasNext()) {
                            sendResult = producer.send(batch, sendMsgTimeout);
                            processSendResult(sendResult, batch);
                        }
                        break;
                }
            } catch (Exception e) {
                if (email) {
                    try {
                        sendEmail.sendAnEmail(null, msg, e);
                    } catch (Exception exception) {
                        System.out.println("send email failed");
                        System.out.println("message: "+s);
                        exception.printStackTrace();
                    }
                } else {
                    System.out.println("send message failed: "+s);
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void stop() {
        stopped = true;
        done.countDown();
    }

    @Override
    public void awaitStop() throws InterruptedException {
        done.await();
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        // should return a list of all configuration options for this plugin
        return PluginConfigParams.paramsList;
//        return Collections.singletonList(PREFIX_CONFIG);
    }

    @Override
    public String getId() {
        return id;
    }

    public void processSendResult(SendResult sendResult, Message msg) {
        if (sendResult == null) {
            System.out.println("send failed: " + msg);
            return;
        }
        SendStatus sendStatus = sendResult.getSendStatus();
        if (!(sendStatus == SendStatus.SEND_OK)) {
            if (email) {
                try {
                    sendEmail.sendAnEmail(sendResult, msg, null);
                    System.out.println("sent an email");
                } catch (Exception exception) {
                    System.out.println("send email failed");
                    System.out.println("message: "+new String(msg.getBody()));
                    exception.printStackTrace();
                }
            } else {
                System.out.println("message: "+new String(msg.getBody()));
                System.out.println(sendResult.toString());
            }
        }
    }

    public void processSendResult(SendResult sendResult, Collection<Message> batchMsg) {
        if (sendResult == null) {
            System.out.println("send failed: " + batchMsg);
            return;
        }
        SendStatus sendStatus = sendResult.getSendStatus();
        if (!(sendStatus == SendStatus.SEND_OK)) {
            if (email) {
                try {
                    sendEmail.sendAnEmail(sendResult, null, null);
                    System.out.println("sent an email");
                } catch (Exception exception) {
                    System.out.println("send email failed");
                    exception.printStackTrace();
                }
            } else {
                System.out.println(sendResult.toString());
            }
        }
    }
}
