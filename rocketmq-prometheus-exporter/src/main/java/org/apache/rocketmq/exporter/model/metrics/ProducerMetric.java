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
package org.apache.rocketmq.exporter.model.metrics;

public class ProducerMetric {

    private  String   clusterName;
    private  String   brokerName;
    private  String   topicName;

    public void setClusterName(String cluster) {
        clusterName = cluster;
    }
    public  String getClusterName() {
        return clusterName;
    }
    void setBrokerName(String broker) {
        brokerName = broker;
    }
    public String getBrokerName() {
        return brokerName;
    }
    public void setTopicName(String topic) {
        topicName = topic;
    }
    public String  getTopicName() {
        return topicName;
    }
    public ProducerMetric(String cluster,String broker,String topic) {
        clusterName = cluster;
        brokerName  =   broker;
        topicName   =   topic;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ProducerMetric)) {
            return false;
        }
        ProducerMetric other = (ProducerMetric) obj;

        return  other.clusterName.equals(clusterName) && other.brokerName.equals(brokerName)
                && other.topicName.equals(topicName);
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = 37 * hash + clusterName.hashCode();
        hash = 37 * hash + brokerName.hashCode();
        hash = 37 * hash + topicName.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "ClusterName: " + clusterName + " BrokerName: " + brokerName + " topicName: " + topicName;
    }
}
