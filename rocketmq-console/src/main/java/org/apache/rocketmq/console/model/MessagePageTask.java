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

import org.springframework.data.domain.Page;

import java.util.List;

public class MessagePageTask {
    private Page<MessageView> page;
    private List<QueueOffsetInfo> queueOffsetInfos;

    public MessagePageTask(Page<MessageView> page, List<QueueOffsetInfo> queueOffsetInfos) {
        this.page = page;
        this.queueOffsetInfos = queueOffsetInfos;
    }

    public Page<MessageView> getPage() {
        return page;
    }

    public void setPage(Page<MessageView> page) {
        this.page = page;
    }

    public List<QueueOffsetInfo> getQueueOffsetInfos() {
        return queueOffsetInfos;
    }

    public void setQueueOffsetInfos(List<QueueOffsetInfo> queueOffsetInfos) {
        this.queueOffsetInfos = queueOffsetInfos;
    }

    @Override
    public String toString() {
        return "MessagePageTask{" +
                "page=" + page +
                ", queueOffsetInfos=" + queueOffsetInfos +
                '}';
    }
}
