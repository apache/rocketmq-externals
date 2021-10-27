package org.apache.rocketmq.replicator.offset;/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.common.message.MessageQueue;

public class OffsetSync {

    private MessageQueue mq;
    private String group;
    private long srcOffset;
    private long targetOffset;

    public OffsetSync(MessageQueue mq, String group, long srcOffset, long targetOffset) {
        this.mq = mq;
        this.group = group;
        this.srcOffset = srcOffset;
        this.targetOffset = targetOffset;
    }

    public void setMq(MessageQueue mq) {
        this.mq = mq;
    }

    public void setSrcOffset(long srcOffset) {
        this.srcOffset = srcOffset;
    }

    public void setTargetOffset(long targetOffset) {
        this.targetOffset = targetOffset;
    }

    public long getSrcOffset() {
        return this.srcOffset;
    }

    public long getTargetOffset() {
        return this.targetOffset;
    }

    public MessageQueue getMq() {
        return this.mq;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public byte[] encode() {
        return JSON.toJSONBytes(this);
    }

    public static OffsetSync decode(byte[] body) {
        OffsetSync sync = JSON.parseObject(body, OffsetSync.class);
        return sync;
    }
}
