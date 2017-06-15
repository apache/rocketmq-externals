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
package org.apache.rocketmq.console.model.request;

import org.apache.rocketmq.common.subscription.SubscriptionGroupConfig;

import java.util.List;

public class ConsumerConfigInfo {
    private List<String> clusterNameList;

    private List<String> brokerNameList;
    private SubscriptionGroupConfig subscriptionGroupConfig;

    public ConsumerConfigInfo() {
    }

    public ConsumerConfigInfo(List<String> brokerNameList, SubscriptionGroupConfig subscriptionGroupConfig) {
        this.brokerNameList = brokerNameList;
        this.subscriptionGroupConfig = subscriptionGroupConfig;
    }

    public List<String> getClusterNameList() {
        return clusterNameList;
    }

    public void setClusterNameList(List<String> clusterNameList) {
        this.clusterNameList = clusterNameList;
    }

    public List<String> getBrokerNameList() {
        return brokerNameList;
    }

    public void setBrokerNameList(List<String> brokerNameList) {
        this.brokerNameList = brokerNameList;
    }

    public SubscriptionGroupConfig getSubscriptionGroupConfig() {
        return subscriptionGroupConfig;
    }

    public void setSubscriptionGroupConfig(SubscriptionGroupConfig subscriptionGroupConfig) {
        this.subscriptionGroupConfig = subscriptionGroupConfig;
    }

}
