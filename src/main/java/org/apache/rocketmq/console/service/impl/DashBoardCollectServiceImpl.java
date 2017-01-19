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

import com.alibaba.rocketmq.client.exception.MQBrokerException;
import com.alibaba.rocketmq.common.protocol.body.ClusterInfo;
import com.alibaba.rocketmq.common.protocol.body.KVTable;
import com.alibaba.rocketmq.common.protocol.route.BrokerData;
import com.alibaba.rocketmq.remoting.exception.RemotingConnectException;
import com.alibaba.rocketmq.remoting.exception.RemotingSendRequestException;
import com.alibaba.rocketmq.remoting.exception.RemotingTimeoutException;
import com.alibaba.rocketmq.tools.admin.MQAdminExt;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.base.Ticker;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import org.apache.rocketmq.console.service.DashboardCollectService;
import org.apache.rocketmq.console.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DashboardCollectServiceImpl implements DashboardCollectService {

    private final static Logger logger = LoggerFactory.getLogger(DashboardCollectServiceImpl.class);

    private LoadingCache<String, List<BigDecimal>> brokerMap = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .concurrencyLevel(10)
        .recordStats()
        .ticker(Ticker.systemTicker())
        .removalListener(new RemovalListener<Object, Object>() {
            @Override
            public void onRemoval(RemovalNotification<Object, Object> notification) {
                logger.warn(notification.getKey() + " was removed, cause is " + notification.getCause());
            }
        })
        .build(
            new CacheLoader<String, List<BigDecimal>>() {
                @Override
                public List<BigDecimal> load(String key) {
                    List<BigDecimal> list = Lists.newArrayList();
                    return list;
                }
            }
        );
    private Stopwatch stopwatch = Stopwatch.createStarted();

    @Resource
    private MQAdminExt mqAdminExt;

    @Scheduled(cron = "0/5 * *  * * ? ")
    @Override
    public void collectTopic() {
        try {
            ClusterInfo clusterInfo = mqAdminExt.examineBrokerClusterInfo();
            Set<Map.Entry<String, BrokerData>> clusterEntries = clusterInfo.getBrokerAddrTable().entrySet();

            Map<String, Long> addresses = Maps.newHashMap();
            for (Map.Entry<String, BrokerData> clusterEntry : clusterEntries) {
                HashMap<Long, String> addrs = clusterEntry.getValue().getBrokerAddrs();
                Set<Map.Entry<Long, String>> addrsEntries = addrs.entrySet();
                for (Map.Entry<Long, String> addrEntry : addrsEntries) {
                    addresses.put(addrEntry.getValue(), addrEntry.getKey());
                }
            }
            Set<Map.Entry<String, Long>> entries = addresses.entrySet();
            for (Map.Entry<String, Long> entry : entries) {
                List<BigDecimal> list = brokerMap.get(entry.getKey());
                if (null == list) {
                    list = Lists.newArrayList();
                }
                KVTable kvTable = mqAdminExt.fetchBrokerRuntimeStats(entry.getKey());
                list.add(new BigDecimal(kvTable.getTable().get("getTotalTps").split(" ")[0]));
                brokerMap.put(entry.getKey(), list);
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

        logger.error("collect topic >>>>>>");
    }

    @Scheduled(cron = "0/5 * *  * * ? ")
    @Override
    public void collectBroker() {
        logger.error("collect broker >>>>>>");
    }

    @Scheduled(cron = "0/5 * *  * * ? ")
    @Override
    public void saveData() {
        //one day refresh cache one time
        logger.info(JsonUtil.obj2String(brokerMap.asMap()));
        if(stopwatch.elapsed(TimeUnit.DAYS)> 1){
            brokerMap.invalidateAll();
            stopwatch.reset();
        }
    }

    @Override
    public LoadingCache<String, List<BigDecimal>> getBrokerCache() {
        return brokerMap;
    }

}
