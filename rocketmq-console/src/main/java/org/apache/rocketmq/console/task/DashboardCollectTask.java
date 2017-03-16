/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.rocketmq.console.task;

import com.google.common.base.Stopwatch;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.GroupList;
import org.apache.rocketmq.common.protocol.body.KVTable;
import org.apache.rocketmq.common.protocol.body.TopicList;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.console.aspect.admin.annotation.MultiMQAdminCmdMethod;
import org.apache.rocketmq.store.stats.BrokerStatsManager;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.apache.rocketmq.tools.command.stats.StatsAllSubCommand;
import com.google.common.base.Throwables;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.annotation.Resource;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.protocol.body.BrokerStatsData;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.service.DashboardCollectService;
import org.apache.rocketmq.console.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DashboardCollectTask {
    private Date currentDate = new Date();
    @Resource
    private MQAdminExt mqAdminExt;
    @Resource
    private RMQConfigure rmqConfigure;

    @Resource
    private DashboardCollectService dashboardCollectService;

    private final static Logger log = LoggerFactory.getLogger(DashboardCollectTask.class);

    @Scheduled(cron = "30 0/1 * * * ?")
    @MultiMQAdminCmdMethod(timeoutMillis = 5000)
    public void collectTopic() {
        if (!rmqConfigure.isEnableDashBoardCollect()) {
            return;
        }
        Date date = new Date();
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            TopicList topicList = mqAdminExt.fetchAllTopicList();
            Set<String> topicSet = topicList.getTopicList();
            for (String topic : topicSet) {
                if (topic.startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX) || topic.startsWith(MixAll.DLQ_GROUP_TOPIC_PREFIX)) {
                    continue;
                }

                TopicRouteData topicRouteData = mqAdminExt.examineTopicRouteInfo(topic);

                GroupList groupList = mqAdminExt.queryTopicConsumeByWho(topic);

                double inTPS = 0;

                long inMsgCntToday = 0;

                double outTPS = 0;

                long outMsgCntToday = 0;

                for (BrokerData bd : topicRouteData.getBrokerDatas()) {
                    String masterAddr = bd.getBrokerAddrs().get(MixAll.MASTER_ID);
                    if (masterAddr != null) {
                        try {
                            stopwatch.start();
                            log.info("start time: {}", stopwatch.toString());
                            BrokerStatsData bsd = mqAdminExt.viewBrokerStatsData(masterAddr, BrokerStatsManager.TOPIC_PUT_NUMS, topic);
                            stopwatch.stop();
                            log.info("stop time : {}", stopwatch.toString());
                            stopwatch.reset();
                            inTPS += bsd.getStatsMinute().getTps();
                            inMsgCntToday += StatsAllSubCommand.compute24HourSum(bsd);
                        }
                        catch (Exception e) {
//                            throw Throwables.propagate(e);
                        }
                    }
                }

                if (groupList != null && !groupList.getGroupList().isEmpty()) {

                    for (String group : groupList.getGroupList()) {
                        for (BrokerData bd : topicRouteData.getBrokerDatas()) {
                            String masterAddr = bd.getBrokerAddrs().get(MixAll.MASTER_ID);
                            if (masterAddr != null) {
                                try {
                                    String statsKey = String.format("%s@%s", topic, group);
                                    BrokerStatsData bsd = mqAdminExt.viewBrokerStatsData(masterAddr, BrokerStatsManager.GROUP_GET_NUMS, statsKey);
                                    outTPS += bsd.getStatsMinute().getTps();
                                    outMsgCntToday += StatsAllSubCommand.compute24HourSum(bsd);
                                }
                                catch (Exception e) {
//                                    throw Throwables.propagate(e);
                                }
                            }
                        }
                    }
                }

                List<String> list;
                try {
                    list = dashboardCollectService.getTopicMap().get(topic);
                }
                catch (ExecutionException e) {
                    throw Throwables.propagate(e);
                }
                if (null == list) {
                    list = Lists.newArrayList();
                }

                list.add(date.getTime() + "," + new BigDecimal(inTPS).setScale(5, BigDecimal.ROUND_HALF_UP) + "," + inMsgCntToday + "," + new BigDecimal(outTPS).setScale(5, BigDecimal.ROUND_HALF_UP) + "," + outMsgCntToday);
                dashboardCollectService.getTopicMap().put(topic, list);

            }

            log.debug("Topic Collected Data in memory = {}" + JsonUtil.obj2String(dashboardCollectService.getTopicMap().asMap()));
        }
        catch (Exception err) {
            throw Throwables.propagate(err);
        }
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public void collectBroker() {
        if (!rmqConfigure.isEnableDashBoardCollect()) {
            return;
        }
        try {
            Date date = new Date();
            ClusterInfo clusterInfo = mqAdminExt.examineBrokerClusterInfo();
            Set<Map.Entry<String, BrokerData>> clusterEntries = clusterInfo.getBrokerAddrTable().entrySet();

            Map<String, String> addresses = Maps.newHashMap();
            for (Map.Entry<String, BrokerData> clusterEntry : clusterEntries) {
                HashMap<Long, String> addrs = clusterEntry.getValue().getBrokerAddrs();
                Set<Map.Entry<Long, String>> addrsEntries = addrs.entrySet();
                for (Map.Entry<Long, String> addrEntry : addrsEntries) {
                    addresses.put(addrEntry.getValue(), clusterEntry.getKey() + ":" + addrEntry.getKey());
                }
            }
            Set<Map.Entry<String, String>> entries = addresses.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                List<String> list = dashboardCollectService.getBrokerMap().get(entry.getValue());
                if (null == list) {
                    list = Lists.newArrayList();
                }
                KVTable kvTable = fetchBrokerRuntimeStats(entry.getKey(), 3);
                if (kvTable == null) {
                    continue;
                }
                String[] tpsArray = kvTable.getTable().get("getTotalTps").split(" ");
                BigDecimal totalTps = new BigDecimal(0);
                for (String tps : tpsArray) {
                    totalTps = totalTps.add(new BigDecimal(tps));
                }
                BigDecimal averageTps = totalTps.divide(new BigDecimal(tpsArray.length), 5, BigDecimal.ROUND_HALF_UP);
                list.add(date.getTime() + "," + averageTps.toString());
                dashboardCollectService.getBrokerMap().put(entry.getValue(), list);
            }
            log.debug("Broker Collected Data in memory = {}" + JsonUtil.obj2String(dashboardCollectService.getBrokerMap().asMap()));
        }
        catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private KVTable fetchBrokerRuntimeStats(String brokerAddr, Integer retryTime) {
        if (retryTime == 0) {
            return null;
        }
        try {
            return mqAdminExt.fetchBrokerRuntimeStats(brokerAddr);
        }
        catch (Exception e) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e1) {
                throw Throwables.propagate(e1);
            }
            fetchBrokerRuntimeStats(brokerAddr, retryTime - 1);
            throw Throwables.propagate(e);
        }
    }

    @Scheduled(cron = "0/5 * * * * ?")
    public void saveData() {
        if (!rmqConfigure.isEnableDashBoardCollect()) {
            return;
        }
        //one day refresh cache one time
        String dataLocationPath = rmqConfigure.getConsoleCollectData();
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String nowDateStr = format.format(new Date());
        String currentDateStr = format.format(currentDate);
        if (!currentDateStr.equals(nowDateStr)) {
            dashboardCollectService.getBrokerMap().invalidateAll();
            dashboardCollectService.getTopicMap().invalidateAll();
            currentDate = new Date();
        }
        File brokerFile = new File(dataLocationPath + nowDateStr + ".json");
        File topicFile = new File(dataLocationPath + nowDateStr + "_topic" + ".json");
        try {
            Map<String, List<String>> brokerFileMap;
            Map<String, List<String>> topicFileMap;
            if (brokerFile.exists()) {
                brokerFileMap = dashboardCollectService.jsonDataFile2map(brokerFile);
            }
            else {
                brokerFileMap = Maps.newHashMap();
                Files.createParentDirs(brokerFile);
            }

            if (topicFile.exists()) {
                topicFileMap = dashboardCollectService.jsonDataFile2map(topicFile);
            }
            else {
                topicFileMap = Maps.newHashMap();
                Files.createParentDirs(topicFile);
            }

            brokerFile.createNewFile();
            topicFile.createNewFile();

            writeFile(dashboardCollectService.getBrokerMap(), brokerFileMap, brokerFile);
            writeFile(dashboardCollectService.getTopicMap(), topicFileMap, topicFile);
            log.debug("Broker Collected Data in memory = {}" + JsonUtil.obj2String(dashboardCollectService.getBrokerMap().asMap()));
            log.debug("Topic Collected Data in memory = {}" + JsonUtil.obj2String(dashboardCollectService.getTopicMap().asMap()));

        }
        catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private void writeFile(LoadingCache<String, List<String>> map, Map<String, List<String>> fileMap,
        File file) throws IOException {
        Map<String, List<String>> newMap = map.asMap();
        Map<String, List<String>> resultMap = Maps.newHashMap();
        if (fileMap.size() == 0) {
            resultMap = newMap;
        }
        else {
            for (Map.Entry<String, List<String>> entry : fileMap.entrySet()) {
                List<String> oldList = entry.getValue();
                List<String> newList = newMap.get(entry.getKey());
                resultMap.put(entry.getKey(), appendData(newList, oldList));
                if (newList == null || newList.size() == 0) {
                    map.put(entry.getKey(), appendData(newList, oldList));
                }
            }

            for (Map.Entry<String, List<String>> entry : newMap.entrySet()) {
                List<String> oldList = fileMap.get(entry.getKey());
                if (oldList == null || oldList.size() == 0) {
                    resultMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
        Files.write(JsonUtil.obj2String(resultMap).getBytes(), file);
    }

    private List<String> appendData(List<String> newTpsList, List<String> oldTpsList) {
        List<String> result = Lists.newArrayList();
        if (newTpsList == null || newTpsList.size() == 0) {
            return oldTpsList;
        }
        if (oldTpsList == null || oldTpsList.size() == 0) {
            return newTpsList;
        }
        String oldLastTps = oldTpsList.get(oldTpsList.size() - 1);
        Long oldLastTimestamp = Long.parseLong(oldLastTps.split(",")[0]);
        String newFirstTps = newTpsList.get(0);
        Long newFirstTimestamp = Long.parseLong(newFirstTps.split(",")[0]);
        if (oldLastTimestamp.longValue() < newFirstTimestamp.longValue()) {
            result.addAll(oldTpsList);
            result.addAll(newTpsList);
            return result;
        }
        return newTpsList;
    }

}
