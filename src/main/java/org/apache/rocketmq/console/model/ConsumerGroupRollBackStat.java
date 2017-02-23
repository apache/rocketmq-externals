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
package org.apache.rocketmq.console.model;

import org.apache.rocketmq.common.admin.RollbackStats;
import com.google.common.collect.Lists;

import java.util.List;

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
