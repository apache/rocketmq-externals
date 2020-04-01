package org.apache.rocketmq.connect.jdbc.connector;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.Task;
import io.openmessaging.connector.api.sink.SinkConnector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.QueueData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.connect.jdbc.common.CloneUtils;
import org.apache.rocketmq.connect.jdbc.common.ConstDefine;
import org.apache.rocketmq.connect.jdbc.common.Utils;
import org.apache.rocketmq.connect.jdbc.config.Config;
import org.apache.rocketmq.connect.jdbc.config.DataType;
import org.apache.rocketmq.connect.jdbc.config.DbConnectorConfig;
import org.apache.rocketmq.connect.jdbc.config.SinkDbConnectorConfig;
import org.apache.rocketmq.connect.jdbc.config.TaskDivideConfig;
import org.apache.rocketmq.connect.jdbc.config.TaskTopicInfo;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JdbcSinkConnector extends SinkConnector {
    private static final Logger log = LoggerFactory.getLogger(JdbcSinkConnector.class);
    private DbConnectorConfig dbConnectorConfig;
    private volatile boolean configValid = false;
    private ScheduledExecutorService executor;
    private HashMap<String, Set<TaskTopicInfo>> topicRouteMap;

    private DefaultMQAdminExt srcMQAdminExt;

    private volatile boolean adminStarted;

    public JdbcSinkConnector() {
        topicRouteMap = new HashMap<>();
        dbConnectorConfig = new SinkDbConnectorConfig();
        executor = Executors.newSingleThreadScheduledExecutor(new BasicThreadFactory.Builder().namingPattern("JdbcSinkConnector-SinkWatcher-%d").daemon(true).build());
    }

    private synchronized void startMQAdminTools() {
        if (!configValid || adminStarted) {
            return;
        }
        RPCHook rpcHook = null;
        this.srcMQAdminExt = new DefaultMQAdminExt(rpcHook);
        this.srcMQAdminExt.setNamesrvAddr(((SinkDbConnectorConfig) this.dbConnectorConfig).getSrcNamesrvs());
        this.srcMQAdminExt.setAdminExtGroup(Utils.createGroupName(ConstDefine.JDBC_CONNECTOR_ADMIN_PREFIX));
        this.srcMQAdminExt.setInstanceName(Utils.createInstanceName(((SinkDbConnectorConfig) this.dbConnectorConfig).getSrcNamesrvs()));

        try {
            this.srcMQAdminExt.start();
            log.info("RocketMQ srcMQAdminExt started");

        } catch (MQClientException e) {
            log.error("Replicator start failed for `srcMQAdminExt` exception.", e);
        }

        adminStarted = true;
    }

    @Override
    public String verifyAndSetConfig(KeyValue config) {
        for (String requestKey : Config.REQUEST_CONFIG) {
            if (!config.containsKey(requestKey)) {
                return "Request config key: " + requestKey;
            }
        }
        try {
            this.dbConnectorConfig.validate(config);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
        this.configValid = true;

        return "";
    }

    @Override
    public void start() {
        startMQAdminTools();
        startListener();
    }

    public void startListener() {
        executor.scheduleAtFixedRate(new Runnable() {
            boolean first = true;
            HashMap<String, Set<TaskTopicInfo>> origin = null;

            @Override
            public void run() {
                buildRoute();
                if (first) {
                    origin = CloneUtils.clone(topicRouteMap);
                    first = false;
                }
                if (!compare(origin, topicRouteMap)) {
                    context.requestTaskReconfiguration();
                    origin = CloneUtils.clone(topicRouteMap);
                }
            }
        }, ((SinkDbConnectorConfig) dbConnectorConfig).getRefreshInterval(), ((SinkDbConnectorConfig) dbConnectorConfig).getRefreshInterval(), TimeUnit.SECONDS);
    }

    public boolean compare(Map<String, Set<TaskTopicInfo>> origin, Map<String, Set<TaskTopicInfo>> updated) {
        if (origin.size() != updated.size()) {
            return false;
        }
        for (Map.Entry<String, Set<TaskTopicInfo>> entry : origin.entrySet()) {
            if (!updated.containsKey(entry.getKey())) {
                return false;
            }
            Set<TaskTopicInfo> originTasks = entry.getValue();
            Set<TaskTopicInfo> updateTasks = updated.get(entry.getKey());
            if (originTasks.size() != updateTasks.size()) {
                return false;
            }

            if (!originTasks.containsAll(updateTasks)) {
                return false;
            }
        }

        return true;
    }

    public void buildRoute() {
        String srcCluster = ((SinkDbConnectorConfig) this.dbConnectorConfig).getSrcCluster();
        try {
            for (String topic : ((SinkDbConnectorConfig) this.dbConnectorConfig).getWhiteList()) {

                // different from BrokerData with cluster field, which can ensure the brokerData is from expected cluster.
                // QueueData use brokerName as unique info on cluster of rocketmq. so when we want to get QueueData of
                // expected cluster, we should get brokerNames of expected cluster, and then filter queueDatas.
                List<BrokerData> brokerList = Utils.examineBrokerData(this.srcMQAdminExt, topic, srcCluster);
                Set<String> brokerNameSet = new HashSet<String>();
                for (BrokerData b : brokerList) {
                    brokerNameSet.add(b.getBrokerName());
                }

                TopicRouteData topicRouteData = srcMQAdminExt.examineTopicRouteInfo(topic);
                if (!topicRouteMap.containsKey(topic)) {
                    topicRouteMap.put(topic, new HashSet<>(16));
                }
                for (QueueData qd : topicRouteData.getQueueDatas()) {
                    if (brokerNameSet.contains(qd.getBrokerName())) {
                        for (int i = 0; i < qd.getReadQueueNums(); i++) {
                            TaskTopicInfo taskTopicInfo = new TaskTopicInfo(topic, qd.getBrokerName(), i, null);
                            topicRouteMap.get(topic).add(taskTopicInfo);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Fetch topic list error.", e);
        } finally {
            srcMQAdminExt.shutdown();
        }
    }

    @Override
    public void stop() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public Class<? extends Task> taskClass() {
        return JdbcSinkTask.class;
    }

    @Override
    public List<KeyValue> taskConfigs() {
        log.info("List.start");
        if (!configValid) {
            return new ArrayList<KeyValue>();
        }

        startMQAdminTools();

        buildRoute();

        TaskDivideConfig tdc = new TaskDivideConfig(
                this.dbConnectorConfig.getDbUrl(),
                this.dbConnectorConfig.getDbPort(),
                this.dbConnectorConfig.getDbUserName(),
                this.dbConnectorConfig.getDbPassword(),
                this.dbConnectorConfig.getConverter(),
                DataType.COMMON_MESSAGE.ordinal(),
                this.dbConnectorConfig.getTaskParallelism(),
                this.dbConnectorConfig.getMode()
        );

        ((SinkDbConnectorConfig) this.dbConnectorConfig).setTopicRouteMap(topicRouteMap);

        return this.dbConnectorConfig.getTaskDivideStrategy().divide(this.dbConnectorConfig, tdc);
    }
}
