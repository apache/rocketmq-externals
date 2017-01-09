package org.apache.rocketmq.console.model;

import com.alibaba.rocketmq.common.admin.RollbackStats;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by tangjie
 * 2016/11/23
 * styletang.me@gmail.com
 */
public class ConsumerGroupRollBackStat {
    private boolean status;
    private String errMsg;
    private List<RollbackStats> rollbackStatsList = Lists.newArrayList();

    public ConsumerGroupRollBackStat(boolean status) {
        this.status = status;
    }

    public ConsumerGroupRollBackStat(boolean status, String errMsg) {
        this.status = status;
        this.errMsg = errMsg;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public List<RollbackStats> getRollbackStatsList() {
        return rollbackStatsList;
    }

    public void setRollbackStatsList(List<RollbackStats> rollbackStatsList) {
        this.rollbackStatsList = rollbackStatsList;
    }
}
