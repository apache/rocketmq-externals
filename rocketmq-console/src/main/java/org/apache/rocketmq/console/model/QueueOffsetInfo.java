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

import org.apache.rocketmq.common.message.MessageQueue;

public class QueueOffsetInfo {
    private Integer idx;

    private Long start;
    private Long end;

    private Long startOffset;
    private Long endOffset;
    private MessageQueue messageQueues;

    public QueueOffsetInfo() {
    }

    public QueueOffsetInfo(Integer idx, Long start, Long end, Long startOffset, Long endOffset, MessageQueue messageQueues) {
        this.idx = idx;
        this.start = start;
        this.end = end;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.messageQueues = messageQueues;
    }

    public Integer getIdx() {
        return idx;
    }

    public void setIdx(Integer idx) {
        this.idx = idx;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }

    public Long getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(Long startOffset) {
        this.startOffset = startOffset;
    }

    public Long getEndOffset() {
        return endOffset;
    }

    public void setEndOffset(Long endOffset) {
        this.endOffset = endOffset;
    }

    public MessageQueue getMessageQueues() {
        return messageQueues;
    }

    public void setMessageQueues(MessageQueue messageQueues) {
        this.messageQueues = messageQueues;
    }

    public void incStartOffset() {
        this.startOffset++;
        this.endOffset++;
    }

    public void incEndOffset() {
        this.endOffset++;
    }

    public void incStartOffset(long size) {
        this.startOffset += size;
        this.endOffset += size;
    }
}
