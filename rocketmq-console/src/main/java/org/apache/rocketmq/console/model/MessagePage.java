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

public class MessagePage {
    private Page<MessageView> page;
    private String taskId;

    public MessagePage(Page<MessageView> page, String taskId) {
        this.page = page;
        this.taskId = taskId;
    }

    public Page<MessageView> getPage() {
        return page;
    }

    public void setPage(Page<MessageView> page) {
        this.page = page;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public String toString() {
        return "MessagePage{" +
                "page=" + page +
                ", taskId='" + taskId + '\'' +
                '}';
    }
}
