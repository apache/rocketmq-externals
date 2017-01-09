package org.apache.rocketmq.console.model.request;

import java.util.List;

/**
 * Created by tangjie
 * 2016/11/29
 * styletang.me@gmail.com
 */
public class DeleteSubGroupRequest {
    private String groupName;
    private List<String> brokerNameList;
//    private List<String> clusterList; // todo use

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<String> getBrokerNameList() {
        return brokerNameList;
    }

    public void setBrokerNameList(List<String> brokerNameList) {
        this.brokerNameList = brokerNameList;
    }
}
