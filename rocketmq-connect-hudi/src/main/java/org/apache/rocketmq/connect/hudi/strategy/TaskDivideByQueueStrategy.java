package org.apache.rocketmq.connect.hudi.strategy;

import com.alibaba.fastjson.JSONObject;
import io.openmessaging.KeyValue;
import io.openmessaging.internal.DefaultKeyValue;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.connect.hudi.config.HudiConnectConfig;

import java.util.*;

import static org.apache.rocketmq.connect.hudi.config.HudiConnectConfig.*;

/**
 * @author osgoo
 * @date 2021/9/2
 */
public class TaskDivideByQueueStrategy implements ITaskDivideStrategy {
    @Override
    public List<KeyValue> divide(KeyValue source) {
        List<KeyValue> config = new ArrayList<KeyValue>();
        int parallelism = source.getInt(HudiConnectConfig.CONN_TASK_PARALLELISM);
        Map<String, MessageQueue> topicRouteInfos = (Map<String, MessageQueue>) JSONObject.parse(source.getString(HudiConnectConfig.CONN_TOPIC_ROUTE_INFO));
        int id = 0;
        List<List<String>> taskTopicQueues = new ArrayList<>(parallelism);
        for (Map.Entry<String, MessageQueue> topicQueue : topicRouteInfos.entrySet()) {
            MessageQueue messageQueue = topicQueue.getValue();
            String topicQueueStr = messageQueue.getTopic() + "," + messageQueue.getBrokerName() + "," + messageQueue.getQueueId();
            int ind = ++id % parallelism;
            if (taskTopicQueues.get(ind) != null) {
                List<String> taskTopicQueue = new LinkedList<>();
                taskTopicQueue.add(topicQueueStr);
                taskTopicQueues.add(ind, taskTopicQueue);
            } else {
                List<String> taskTopicQueue = taskTopicQueues.get(ind);
                taskTopicQueue.add(topicQueueStr);
            }
        }

        for (int i = 0; i < parallelism; i++) {
            // build single task queue config; format is topicName1,brokerName1,queueId1;topicName1,brokerName1,queueId2
            String singleTaskTopicQueueStr = "";
            List<String> singleTaskTopicQueues = taskTopicQueues.get(i);
            for(String singleTopicQueue : singleTaskTopicQueues) {
                singleTaskTopicQueueStr += singleTopicQueue + ";";
            }
            singleTaskTopicQueueStr = singleTaskTopicQueueStr.substring(0, singleTaskTopicQueueStr.length() - 1);
            // fill connect config;
            KeyValue keyValue = new DefaultKeyValue();
            keyValue.put(CONN_TOPIC_QUEUES, singleTaskTopicQueueStr);
            keyValue.put(CONN_HUDI_TABLE_PATH, source.getString(CONN_HUDI_TABLE_PATH));
            keyValue.put(CONN_HUDI_TABLE_NAME, source.getString(CONN_HUDI_TABLE_NAME));
            keyValue.put(CONN_HUDI_INSERT_SHUFFLE_PARALLELISM, source.getInt(CONN_HUDI_INSERT_SHUFFLE_PARALLELISM));
            keyValue.put(CONN_HUDI_UPSERT_SHUFFLE_PARALLELISM, source.getInt(CONN_HUDI_UPSERT_SHUFFLE_PARALLELISM));
            keyValue.put(CONN_HUDI_DELETE_PARALLELISM, source.getInt(CONN_HUDI_DELETE_PARALLELISM));
            keyValue.put(CONN_SOURCE_RECORD_CONVERTER, source.getString(CONN_SOURCE_RECORD_CONVERTER));
            keyValue.put(CONN_SCHEMA_PATH, source.getString(CONN_SCHEMA_PATH));
            keyValue.put(CONN_TASK_PARALLELISM, source.getInt(CONN_TASK_PARALLELISM));
            keyValue.put(CONN_SCHEMA_PATH, source.getString(CONN_SCHEMA_PATH));
            config.add(keyValue);
        }

        return config;
    }
}
