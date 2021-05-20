package org.apache.rocketmq.iot.benchmark;

import java.util.ArrayList;
import java.util.List;
import org.apache.rocketmq.iot.benchmark.util.ConcurrentTools;
import org.apache.rocketmq.iot.benchmark.util.ConsumerTask;
import org.apache.rocketmq.iot.benchmark.util.ProducerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttDelayedTest {
    private static Logger logger = LoggerFactory.getLogger(MqttDelayedTest.class);

    private String topic_prefix = "mqtt_01/dt_";
    private String client_consumer_prefix = "dc_c_";
    private String client_producer_prefix = "dc_p_";

    private String broker;
    private String password;
    private long productInterval;
    private long timeout;

    public MqttDelayedTest(String broker, String password, long productInterval, long timeout) {
        this.broker = broker;
        this.password = password;
        this.productInterval = productInterval;
        this.timeout = timeout;
    }

    public void process() throws Exception {
        ConcurrentTools tools = new ConcurrentTools();
        List<Runnable> taskList = new ArrayList<>(2);
        taskList.add(initConsumerTask(0));
        taskList.add(initProducerTask(0));
        tools.submitTask(taskList, timeout);
    }

    public ConsumerTask initConsumerTask(int index) {
        String clientId = client_consumer_prefix + index;
        String topic = topic_prefix + index;
        ConsumerTask consumerTask = new ConsumerTask(broker, password, clientId, topic, true);
        return consumerTask;
    }

    public ProducerTask initProducerTask(int index) {
        String clientId = client_producer_prefix + index;
        String topic = topic_prefix + index;
        return new ProducerTask(broker, password, clientId, topic, productInterval, true);
    }

    public static void main(String[] args) throws Exception {
        if (args.length >= 4) {
            String broker = args[0].trim();
            String password = args[1].trim();
            long productInterval = Long.parseLong(args[2].trim());
            long timeout = Integer.parseInt(args[3].trim());

            logger.info("broker:" + broker);
            logger.info("password:" + password);
            logger.info("product interval:" + productInterval);
            logger.info("timeout:" + timeout);

            MqttDelayedTest mqttDelayed = new MqttDelayedTest(broker, password, productInterval, timeout);
            mqttDelayed.process();
        } else {
            System.out.println("args length must more than 4.");
            System.exit(-1);
        }
    }
}
