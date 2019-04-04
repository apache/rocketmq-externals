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
package org.apache.rocketmq.exporter.collector;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.GaugeMetricFamily;
import org.apache.rocketmq.exporter.model.metrics.BrokerMetric;
import org.apache.rocketmq.exporter.model.metrics.ConsumerMetric;
import org.apache.rocketmq.exporter.model.metrics.ConsumerQueueMetric;
import org.apache.rocketmq.exporter.model.metrics.ProducerMetric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RMQMetricsCollector extends Collector {

    private ConcurrentHashMap<ProducerMetric, Double>   topicPutNums            = new ConcurrentHashMap<>();
    private ConcurrentHashMap<ProducerMetric, Double>   topicPutSize            = new ConcurrentHashMap<>();

    private ConcurrentHashMap<ProducerMetric, Double>   topicOffset              = new ConcurrentHashMap<>();

    private ConcurrentHashMap<BrokerMetric, Double>     brokerPutNums           = new ConcurrentHashMap<>();
    private ConcurrentHashMap<BrokerMetric, Double>     brokerGetNums           = new ConcurrentHashMap<>();

    private ConcurrentHashMap<ConsumerMetric, Double>   groupGetNums            = new ConcurrentHashMap<>();
    private ConcurrentHashMap<ConsumerMetric, Double>   groupGetSize            = new ConcurrentHashMap<>();

    private ConcurrentHashMap<ConsumerMetric, Double>       sendBackNums        = new ConcurrentHashMap<>();
    private ConcurrentHashMap<ConsumerMetric, Double>       groupOffset         = new ConcurrentHashMap<>();

    private ConcurrentHashMap<ConsumerQueueMetric, Double>  groupGetLatency     = new ConcurrentHashMap<>();

    private ConcurrentHashMap<ConsumerMetric, Double>  groupGetLatencyByStoreTime     = new ConcurrentHashMap<>();

    @Override
    public List<MetricFamilySamples> collect() {

        List<MetricFamilySamples> mfs = new ArrayList<MetricFamilySamples>();

        GaugeMetricFamily topicPutNumsGauge = new GaugeMetricFamily("rocketmq_producer_tps", "TopicPutNums", Arrays.asList("cluster","broker","topic"));
        for (Map.Entry<ProducerMetric,Double> entry:topicPutNums.entrySet()) {
            topicPutNumsGauge.addMetric(Arrays.asList(entry.getKey().getClusterName(),entry.getKey().getBrokerName(),entry.getKey().getTopicName()), entry.getValue());
        }
        mfs.add(topicPutNumsGauge);


        GaugeMetricFamily topicPutSizeGauge = new GaugeMetricFamily("rocketmq_producer_put_size", "TopicPutSize", Arrays.asList("cluster","broker","topic"));
        for (Map.Entry<ProducerMetric, Double> entry: topicPutSize.entrySet()) {
            topicPutSizeGauge.addMetric(Arrays.asList(entry.getKey().getClusterName(),entry.getKey().getBrokerName(),entry.getKey().getTopicName()), entry.getValue());
        }
        mfs.add(topicPutSizeGauge);


        CounterMetricFamily topicOffsetGauge = new CounterMetricFamily("rocketmq_producer_offset", "TopicOffset", Arrays.asList("cluster","broker","topic"));
        for (Map.Entry<ProducerMetric, Double> entry: topicOffset.entrySet()) {
            topicOffsetGauge.addMetric(Arrays.asList(entry.getKey().getClusterName(),entry.getKey().getBrokerName(),entry.getKey().getTopicName()), entry.getValue());
        }
        mfs.add(topicOffsetGauge);


        GaugeMetricFamily brokerPutNumsGauge = new GaugeMetricFamily("rocketmq_broker_tps", "BrokerPutNums", Arrays.asList("cluster","broker"));
        for (Map.Entry<BrokerMetric, Double> entry: brokerPutNums.entrySet()) {
            brokerPutNumsGauge.addMetric(Arrays.asList(entry.getKey().getClusterName(),entry.getKey().getBrokerName()), entry.getValue());
        }
        mfs.add(brokerPutNumsGauge);


        GaugeMetricFamily brokerGetNumsGauge = new GaugeMetricFamily("rocketmq_broker_qps", "BrokerGetNums", Arrays.asList("cluster","broker"));
        for (Map.Entry<BrokerMetric, Double> entry: brokerGetNums.entrySet()) {
            brokerGetNumsGauge.addMetric(Arrays.asList(entry.getKey().getClusterName(),entry.getKey().getBrokerName()), entry.getValue());
        }
        mfs.add(brokerGetNumsGauge);


        GaugeMetricFamily groupGetNumsGauge = new GaugeMetricFamily("rocketmq_consumer_tps", "GroupGetNums", Arrays.asList("cluster","broker","topic","group"));
        for (Map.Entry<ConsumerMetric, Double> entry: groupGetNums.entrySet()) {
            groupGetNumsGauge.addMetric(Arrays.asList(entry.getKey().getClusterName(),entry.getKey().getBrokerName(),entry.getKey().getTopicName(),entry.getKey().getConsumerGroupName()), entry.getValue());
        }

        mfs.add(groupGetNumsGauge);


        GaugeMetricFamily groupGetSizeGauge = new GaugeMetricFamily("rocketmq_consumer_get_size", "GroupGetSize", Arrays.asList("cluster","broker","topic","group"));
        for (Map.Entry<ConsumerMetric, Double> entry: groupGetSize.entrySet()) {
            groupGetSizeGauge.addMetric(Arrays.asList(entry.getKey().getClusterName(),entry.getKey().getBrokerName(),entry.getKey().getTopicName(),entry.getKey().getConsumerGroupName()), entry.getValue());
        }
        mfs.add(groupGetSizeGauge);

        CounterMetricFamily groupOffsetGauge = new CounterMetricFamily("rocketmq_consumer_offset", "GroupOffset", Arrays.asList("cluster","broker","topic","group"));
        for (Map.Entry<ConsumerMetric, Double> entry: groupOffset.entrySet()) {
            groupOffsetGauge.addMetric(Arrays.asList(entry.getKey().getClusterName(),entry.getKey().getBrokerName(),entry.getKey().getTopicName(),entry.getKey().getConsumerGroupName()), entry.getValue());
        }
        mfs.add(groupOffsetGauge);


        GaugeMetricFamily sendBackNumsGauge = new GaugeMetricFamily("rocketmq_send_back_nums", "SendBackNums", Arrays.asList("cluster","broker","topic","group"));
        for (Map.Entry<ConsumerMetric, Double> entry: sendBackNums.entrySet()) {
            sendBackNumsGauge.addMetric(Arrays.asList(entry.getKey().getClusterName(),entry.getKey().getBrokerName(),entry.getKey().getTopicName(),entry.getKey().getConsumerGroupName()), entry.getValue());
        }
        mfs.add(sendBackNumsGauge);


        GaugeMetricFamily groupGetLatencyGauge = new GaugeMetricFamily("rocketmq_group_get_latency", "GroupGetLatency", Arrays.asList("cluster","broker","topic","group","queueid"));
        for (Map.Entry<ConsumerQueueMetric, Double> entry: groupGetLatency.entrySet()) {
            groupGetLatencyGauge.addMetric(Arrays.asList(entry.getKey().getClusterName(),entry.getKey().getBrokerName(),entry.getKey().getTopicName(),entry.getKey().getConsumerGroupName(),entry.getKey().getQueueId()), entry.getValue());
        }
        mfs.add(groupGetLatencyGauge);

        GaugeMetricFamily groupGetLatencyByStoretimeGauge = new GaugeMetricFamily("rocketmq_group_get_latency_by_storetime", "GroupGetLatencyByStoreTime", Arrays.asList("cluster","broker","topic","group"));
        for (Map.Entry<ConsumerMetric, Double> entry: groupGetLatencyByStoreTime.entrySet()) {
            groupGetLatencyByStoretimeGauge.addMetric(Arrays.asList(entry.getKey().getClusterName(),entry.getKey().getBrokerName(),entry.getKey().getTopicName(),entry.getKey().getConsumerGroupName()), entry.getValue());
        }
        mfs.add(groupGetLatencyByStoretimeGauge);

        return mfs;
    }
    public void AddTopicPutNumsMetric(String clusterName, String brokerName, String topic,  double value)
    {
        topicPutNums.put(new ProducerMetric(clusterName,brokerName,topic),value);
    }

    public void AddTopicPutSizeMetric(String clusterName, String brokerName, String topic,  double value)
    {
        topicPutSize.put(new ProducerMetric(clusterName,brokerName,topic),value);
    }

    public void AddTopicOffsetMetric(String clusterName, String brokerName, String topic,  double value)
    {
        topicOffset.put(new ProducerMetric(clusterName,brokerName,topic),value);
    }

    public void AddBrokerPutNumsMetric(String clusterName, String brokerName,  double value)
    {
        brokerPutNums.put(new BrokerMetric(clusterName,brokerName),value);
    }

    public void AddBrokerGetNumsMetric(String clusterName, String brokerName,  double value)
    {
        brokerGetNums.put(new BrokerMetric(clusterName,brokerName),value);
    }

    public void AddGroupGetNumsMetric(String clusterName, String brokerName, String topic, String group,  double value)
    {
        groupGetNums.put(new ConsumerMetric(clusterName,brokerName,topic,group),value);
    }

    public void AddGroupGetSizeMetric(String clusterName, String brokerName, String topic, String group,  double value)
    {
        groupGetSize.put(new ConsumerMetric(clusterName,brokerName,topic,group),value);
    }

    public void AddGroupOffsetMetric(String clusterName, String brokerName, String topic, String group,  double value)
    {
        groupOffset.put(new ConsumerMetric(clusterName,brokerName,topic,group),value);
    }


    public void AddsendBackNumsMetric(String clusterName, String brokerName, String topic, String group,  double value)
    {
        sendBackNums.put(new ConsumerMetric(clusterName,brokerName,topic,group),value);
    }

    public void AddGroupGetLatencyMetric(String clusterName, String brokerName, String topic, String group, String queueId,double value) {

        groupGetLatency.put(new ConsumerQueueMetric(clusterName,brokerName,topic,group,queueId),value);
    }

    public void AddGroupGetLatencyByStoreTimeMetric(String clusterName, String brokerName, String topic, String group,double value) {

        groupGetLatencyByStoreTime.put(new ConsumerMetric(clusterName,brokerName,topic,group),value);
    }
}