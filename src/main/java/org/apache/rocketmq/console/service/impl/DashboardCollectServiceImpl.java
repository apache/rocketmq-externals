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
package org.apache.rocketmq.console.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.rocketmq.client.exception.MQBrokerException;
import com.alibaba.rocketmq.common.protocol.body.ClusterInfo;
import com.alibaba.rocketmq.common.protocol.body.KVTable;
import com.alibaba.rocketmq.common.protocol.route.BrokerData;
import com.alibaba.rocketmq.remoting.exception.RemotingConnectException;
import com.alibaba.rocketmq.remoting.exception.RemotingSendRequestException;
import com.alibaba.rocketmq.remoting.exception.RemotingTimeoutException;
import com.alibaba.rocketmq.tools.admin.MQAdminExt;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.base.Ticker;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
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
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.exception.ServiceException;
import org.apache.rocketmq.console.service.DashboardCollectService;
import org.apache.rocketmq.console.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DashboardCollectServiceImpl implements DashboardCollectService {

    private final static Logger log = LoggerFactory.getLogger(DashboardCollectServiceImpl.class);

    private LoadingCache<String, List<String>> brokerMap = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .concurrencyLevel(10)
        .recordStats()
        .ticker(Ticker.systemTicker())
        .removalListener(new RemovalListener<Object, Object>() {
            @Override
            public void onRemoval(RemovalNotification<Object, Object> notification) {
                log.warn(notification.getKey() + " was removed, cause is " + notification.getCause());
            }
        })
        .build(
            new CacheLoader<String, List<String>>() {
                @Override
                public List<String> load(String key) {
                    List<String> list = Lists.newArrayList();
                    return list;
                }
            }
        );
    private Date currentDate = new Date();

    @Resource
    private MQAdminExt mqAdminExt;

    @Resource
    private RMQConfigure rmqConfigure;

    @Scheduled(cron = "0/5 * *  * * ? ")
    @Override
    public void collectTopic() {
        log.error("collect topic >>>>>>");
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    @Override
    public void collectBroker() {
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
                List<String> list = brokerMap.get(entry.getValue());
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
                brokerMap.put(entry.getValue(), list);
            }
        }
        catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
        catch (MQBrokerException e) {
            throw Throwables.propagate(e);
        }
        catch (RemotingTimeoutException e) {
            throw Throwables.propagate(e);
        }
        catch (RemotingSendRequestException e) {
            throw Throwables.propagate(e);
        }
        catch (RemotingConnectException e) {
            throw Throwables.propagate(e);
        }
        catch (ExecutionException e) {
            throw Throwables.propagate(e);
        }
        log.error("collect broker >>>>>>");
    }

    private KVTable fetchBrokerRuntimeStats(String brokerAddr, Integer retryTime) {
        if (retryTime.intValue() == 0) {
            return null;
        }
        try {
            return mqAdminExt.fetchBrokerRuntimeStats(brokerAddr);
        }
        catch (RemotingConnectException e) {
            fetchBrokerRuntimeStats(brokerAddr, retryTime - 1);
            throw Throwables.propagate(e);
        }
        catch (RemotingSendRequestException e) {
            fetchBrokerRuntimeStats(brokerAddr, retryTime - 1);
            throw Throwables.propagate(e);
        }
        catch (RemotingTimeoutException e) {
            fetchBrokerRuntimeStats(brokerAddr, retryTime - 1);
            throw Throwables.propagate(e);
        }
        catch (InterruptedException e) {
            fetchBrokerRuntimeStats(brokerAddr, retryTime - 1);
            throw Throwables.propagate(e);
        }
        catch (MQBrokerException e) {
            fetchBrokerRuntimeStats(brokerAddr, retryTime - 1);
            throw Throwables.propagate(e);
        }
    }

    @Scheduled(cron = "0/5 * * * * ?")
    @Override
    public void saveData() {
        //one day refresh cache one time
        log.info(JsonUtil.obj2String(brokerMap.asMap()));
        String dataLocationPath = rmqConfigure.getConsoleCollectData();
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String nowDateStr = format.format(new Date());
        String currentDateStr = format.format(currentDate);
        if (!currentDateStr.equals(nowDateStr)) {
            brokerMap.invalidateAll();
            currentDate = new Date();
        }
        File file = new File(dataLocationPath + nowDateStr + ".json");
        try {
            Map<String, List<String>> map;
            if (file.exists()) {
                map = jsonDataFile2map(file);
            }
            else {
                map = Maps.newHashMap();
                Files.createParentDirs(file);
            }
            file.createNewFile();
            Map<String, List<String>> newTpsMap = brokerMap.asMap();
            Map<String, List<String>> resultMap = Maps.newHashMap();
            if (map.size() == 0) {
                resultMap = newTpsMap;
            }
            else {
                for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                    List<String> oldTpsList = entry.getValue();
                    List<String> newTpsList = newTpsMap.get(entry.getKey());
                    resultMap.put(entry.getKey(), appendTpsData(newTpsList, oldTpsList));
                    if (newTpsList == null || newTpsList.size() == 0) {
                        brokerMap.put(entry.getKey(), appendTpsData(newTpsList, oldTpsList));
                    }
                }
            }
            Files.write(JsonUtil.obj2String(resultMap).getBytes(), file);
        }
        catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private List<String> appendTpsData(List<String> newTpsList, List<String> oldTpsList) {
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

    private Map<String, List<String>> jsonDataFile2map(File file) {
        List<String> strings;
        try {
            strings = Files.readLines(file, Charsets.UTF_8);
        }
        catch (IOException e) {
            throw Throwables.propagate(e);
        }
        StringBuffer sb = new StringBuffer();
        for (String string : strings) {
            sb.append(string);
        }
        JSONObject json = (JSONObject) JSONObject.parse(sb.toString());
        Set<Map.Entry<String, Object>> entries = json.entrySet();
        Map<String, List<String>> map = Maps.newHashMap();
        for (Map.Entry<String, Object> entry : entries) {
            JSONArray tpsArray = (JSONArray) entry.getValue();
            if (tpsArray == null) {
                continue;
            }
            Object[] tpsStrArray = tpsArray.toArray();
            List<String> tpsList = Lists.newArrayList();
            for (Object tpsObj : tpsStrArray) {
                tpsList.add("" + tpsObj);
            }
            map.put(entry.getKey(), tpsList);
        }
        return map;
    }

    @Override
    public Map<String, List<String>> getBrokerCache(String date) {
        String dataLocationPath = rmqConfigure.getConsoleCollectData();
        File file = new File(dataLocationPath + date + ".json");
        if (!file.exists()) {
            throw Throwables.propagate(new ServiceException(-1, "this date have't date!"));
        }
        return jsonDataFile2map(file);
    }

}
