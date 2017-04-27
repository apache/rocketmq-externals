package org.apache.rocketmq.spark;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.rocketmq.broker.BrokerController;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.BrokerConfig;
import org.apache.rocketmq.common.MQVersion;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.namesrv.NamesrvConfig;
import org.apache.rocketmq.namesrv.NamesrvController;
import org.apache.rocketmq.remoting.netty.NettyClientConfig;
import org.apache.rocketmq.remoting.netty.NettyServerConfig;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.store.config.MessageStoreConfig;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author xiaojian.fxj
 * @since 17/3/27
 */
public class RocketMqUtilsTest implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(RocketMqUtilsTest.class);

    private static String NAME_SERVER = "localhost:9876";

    private  static NamesrvController namesrvController;
    private  static BrokerController brokerController;
    private static String TOPIC_DEFAULT = UUID.randomUUID().toString();
    private static String PRODUCER_GROUP = "PRODUCER_GROUP_TEST";

    private static int MESSAGE_NUM = 100;

    @BeforeClass
    public static void startMQ() throws Exception {

        //start nameserver
        startNamesrv();
        //start broker
        startBroker();
        Thread.sleep(2000);

        //producer message
        producer();
    }

    private static void startNamesrv() throws Exception {

        NamesrvConfig namesrvConfig = new NamesrvConfig();
        NettyServerConfig nettyServerConfig = new NettyServerConfig();
        nettyServerConfig.setListenPort(9876);

        namesrvController = new NamesrvController(namesrvConfig, nettyServerConfig);
        boolean initResult = namesrvController.initialize();
        if (!initResult) {
            namesrvController.shutdown();
            throw new Exception();
        }
        namesrvController.start();
    }

    private static void startBroker() throws Exception {

        System.setProperty(RemotingCommand.REMOTING_VERSION_KEY, Integer.toString(MQVersion.CURRENT_VERSION));

        BrokerConfig brokerConfig = new BrokerConfig();
        brokerConfig.setNamesrvAddr(NAME_SERVER);
        brokerConfig.setBrokerId(MixAll.MASTER_ID);
        NettyServerConfig nettyServerConfig = new NettyServerConfig();
        nettyServerConfig.setListenPort(10911);
        NettyClientConfig nettyClientConfig = new NettyClientConfig();
        MessageStoreConfig messageStoreConfig = new MessageStoreConfig();

        brokerController = new BrokerController(brokerConfig, nettyServerConfig, nettyClientConfig, messageStoreConfig);
        boolean initResult = brokerController.initialize();
        if (!initResult) {
            brokerController.shutdown();
            throw new Exception();
        }
        brokerController.start();
    }

    private static void producer() throws Exception {
        // publish test message
        DefaultMQProducer producer = new DefaultMQProducer(PRODUCER_GROUP);
        producer.setNamesrvAddr(NAME_SERVER);

        String sendMsg = "\"Hello Flume\"" + "," + DateFormatUtils.format(new Date(), "yyyy-MM-DD hh:mm:ss");

        try {
            producer.start();
            for (int i = 0; i < MESSAGE_NUM; i++) {
                producer.send(new Message(TOPIC_DEFAULT, sendMsg.getBytes("UTF-8")));

            }
        } catch (Exception e) {
            throw new MQClientException("Failed to publish messages", e);
        } finally {
            producer.shutdown();
        }

        Thread.sleep(2000);
    }

    @AfterClass
    public static void stop() {

        if (brokerController != null) {
            brokerController.shutdown();
        }

        if (namesrvController != null) {
            namesrvController.shutdown();
        }
    }

    @Test
    public void testConsumer() throws MQBrokerException, MQClientException, InterruptedException, UnsupportedEncodingException {

        // start up spark
        Map<String, String> optionParams = new HashMap<>();
        optionParams.put(ConsumerConfig.ROCKETMQ_NAME_SERVER, NAME_SERVER);
        SparkConf sparkConf = new SparkConf().setAppName("JavaCustomReceiver").setMaster("local[*]");
        JavaStreamingContext sc = new JavaStreamingContext(sparkConf, new Duration(1000));
        List<String> topics = new ArrayList<>();
        topics.add(TOPIC_DEFAULT);

        LocationStrategy locationStrategy = LocationStrategy.PreferConsistent();

        JavaInputDStream<MessageExt> stream = RocketMqUtils.createJavaMQPullStream(sc, UUID.randomUUID().toString(),
                topics, ConsumerStrategy.earliest(), false, false, false, locationStrategy, optionParams);


        final Set<MessageExt> result = Collections.synchronizedSet(new HashSet<MessageExt>());

        stream.foreachRDD(new VoidFunction<JavaRDD<MessageExt>>() {
            @Override
            public void call(JavaRDD<MessageExt> messageExtJavaRDD) throws Exception {
                result.addAll(messageExtJavaRDD.collect());
            }
        });

        sc.start();

        long startTime = System.currentTimeMillis();
        boolean matches = false;
        while (!matches && System.currentTimeMillis() - startTime < 20000) {
            matches = MESSAGE_NUM == result.size();
            Thread.sleep(50);
        }
        sc.stop();
    }

    @Test
    public void testGetOffsets() throws MQBrokerException, MQClientException, InterruptedException, UnsupportedEncodingException {

        Map<String, String> optionParams = new HashMap<>();
        optionParams.put(ConsumerConfig.ROCKETMQ_NAME_SERVER, NAME_SERVER);
        SparkConf sparkConf = new SparkConf().setAppName("JavaCustomReceiver").setMaster("local[*]");
        JavaStreamingContext sc = new JavaStreamingContext(sparkConf, new Duration(1000));
        List<String> topics = new ArrayList<>();
        topics.add(TOPIC_DEFAULT);

        LocationStrategy locationStrategy = LocationStrategy.PreferConsistent();

        JavaInputDStream<MessageExt> dStream = RocketMqUtils.createJavaMQPullStream(sc, UUID.randomUUID().toString(),
                topics, ConsumerStrategy.earliest(), false, false, false, locationStrategy, optionParams);

        // hold a reference to the current offset ranges, so it can be used downstream
        final AtomicReference<Map<TopicQueueId, OffsetRange[]>> offsetRanges = new AtomicReference<>();

        final Set<MessageExt> result = Collections.synchronizedSet(new HashSet<MessageExt>());

        dStream.transform(new Function<JavaRDD<MessageExt>, JavaRDD<MessageExt>>() {
            @Override
            public JavaRDD<MessageExt> call(JavaRDD<MessageExt> v1) throws Exception {
                Map<TopicQueueId, OffsetRange []> offsets = ((HasOffsetRanges) v1.rdd()).offsetRanges();
                offsetRanges.set(offsets);
                return v1;
            }
        }).foreachRDD(new VoidFunction<JavaRDD<MessageExt>>() {
            @Override
            public void call(JavaRDD<MessageExt> messageExtJavaRDD) throws Exception {
                result.addAll(messageExtJavaRDD.collect());
            }
        });

        sc.start();

        long startTime = System.currentTimeMillis();
        boolean matches = false;
        while (!matches && System.currentTimeMillis() - startTime < 20000) {
            matches = MESSAGE_NUM == result.size();
            Thread.sleep(50);
        }
        sc.stop();


    }
}
