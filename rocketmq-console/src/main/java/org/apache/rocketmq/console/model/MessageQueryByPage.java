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


import org.springframework.data.domain.PageRequest;

public class MessageQueryByPage {
    public static final int DEFAULT_PAGE = 0;

    public static final int MIN_PAGE_SIZE = 10;

    public static final int MAX_PAGE_SIZE = 100;

    /**
     * current page num
     */
    private int pageNum;

    private int pageSize;

    private String topic;
    private long begin;
    private long end;

    public MessageQueryByPage(int pageNum, int pageSize, String topic, long begin, long end) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.topic = topic;
        this.begin = begin;
        this.end = end;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public long getBegin() {
        return begin;
    }

    public void setBegin(long begin) {
        this.begin = begin;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public int getPageNum() {
        return pageNum <= 0 ? DEFAULT_PAGE : pageNum - 1;
    }

    public int getPageSize() {
        if (pageSize <= 1) {
            return MIN_PAGE_SIZE;
        } else if (pageSize > MAX_PAGE_SIZE) {
            return MAX_PAGE_SIZE;
        }
        return this.pageSize;
    }

    public PageRequest page() {
        return PageRequest.of(this.getPageNum(), this.getPageSize());
    }

    @Override
    public String toString() {
        return "MessageQueryByPage{" +
                "pageNum=" + pageNum +
                ", pageSize=" + pageSize +
                ", topic='" + topic + '\'' +
                ", begin=" + begin +
                ", end=" + end +
                '}';
    }
}
