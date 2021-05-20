package org.apache.rocketmq.iot.benchmark;

import java.util.ArrayList;
import java.util.List;
import org.apache.rocketmq.iot.benchmark.util.ConcurrentTools;
import org.apache.rocketmq.iot.benchmark.util.ConsumerTask;
import org.apache.rocketmq.iot.benchmark.util.ProducerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttPressureTest {
    private static Logger logger = LoggerFactory.getLogger(MqttPressureTest.class);

    private final String client_consumer_prefix = "c_c_";
    private final String client_producer_prefix = "c_p_";

    private String broker;
    private String password;
    private String[] topicList;
    private int taskNumber;
    private long producerInterval;
    private String msg;
    private long timeout;

    public MqttPressureTest(String broker, String password, int taskNumber, String rmqTopic,
        long producerInterval, String msg, long timeout) {
        this.broker = broker;
        this.password = password;
        this.taskNumber = taskNumber;
        this.topicList = rmqTopic.split(",");
        this.producerInterval = producerInterval;
        this.msg = msg;
        this.timeout = timeout;
    }

    public void process() throws Exception {
        ConcurrentTools tools = new ConcurrentTools();
        List<Runnable> taskList = new ArrayList<>(taskNumber * 2);
        for (int i = 1; i <= taskNumber; i++) {
            String topic = topicList[(i % topicList.length)] + "/t_" + i;
            taskList.add(initConsumerTask(topic, i));
            taskList.add(initProducerTask(topic, i));
        }
        tools.submitTask(taskList, timeout);
    }

    public ConsumerTask initConsumerTask(String topic, int index) {
        String clientId = client_consumer_prefix + index;
        return new ConsumerTask(broker, password, clientId, topic);
    }

    public ProducerTask initProducerTask(String topic, int index) {
        String clientId = client_producer_prefix + index;
        return new ProducerTask(broker, password, clientId, topic, producerInterval, msg);
    }

    public static void main(String[] args) throws Exception {
        if (args.length >= 7) {
            String broker = args[0].trim();
            String password = args[1].trim();
            int taskNumber = Integer.parseInt(args[2].trim());
            String rmqTopic = args[3].trim();
            long productInterval = Long.parseLong(args[4].trim());
            String msg = args[5].trim();
            long timeout = Integer.parseInt(args[6].trim());

            logger.info("broker:" + broker);
            logger.info("password:" + password);
            logger.info("task number:" + taskNumber);
            logger.info("rmq topic:" + rmqTopic);
            logger.info("product interval:" + productInterval);
            logger.info("msg:" + msg);
            logger.info("timeout:" + timeout);

            MqttPressureTest pressureTest = new MqttPressureTest(broker, password, taskNumber,
                rmqTopic, productInterval, msg, timeout);
            pressureTest.process();
        } else {
            logger.info("args length must more than 7.");
            System.exit(-1);
        }
    }
}
