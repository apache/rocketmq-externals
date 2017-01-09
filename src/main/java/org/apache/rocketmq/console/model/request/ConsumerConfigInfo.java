package org.apache.rocketmq.console.model.request;

import com.alibaba.rocketmq.common.subscription.SubscriptionGroupConfig;

import java.util.List;

/**
 * Created by tangjie
 * 2016/11/29
 * styletang.me@gmail.com
 */
public class ConsumerConfigInfo {
    //哪个组的 创建的时候使用
    private int selectedOrganization;

    private List<String> brokerNameList;
    private SubscriptionGroupConfig subscriptionGroupConfig;

    public ConsumerConfigInfo() {
    }

    public ConsumerConfigInfo(List<String> brokerNameList, SubscriptionGroupConfig subscriptionGroupConfig) {
        this.brokerNameList = brokerNameList;
        this.subscriptionGroupConfig = subscriptionGroupConfig;
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

    public int getSelectedOrganization() {
        return selectedOrganization;
    }

    public void setSelectedOrganization(int selectedOrganization) {
        this.selectedOrganization = selectedOrganization;
    }
}
