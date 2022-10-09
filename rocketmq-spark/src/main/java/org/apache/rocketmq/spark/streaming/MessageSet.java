/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.spark.streaming;

import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * A message collection.
 */
public class MessageSet implements Iterator<Message>, Serializable {
    private final String id;
    private final List<MessageExt> data;
    private final Iterator<MessageExt> iterator;
    private long timestamp;
    private int retries;

    public MessageSet(String id, List<MessageExt> data) {
        this.id = id;
        this.data = data;
        this.iterator = data.iterator();
    }

    public MessageSet(List<MessageExt> data) {
        this(UUID.randomUUID().toString(), data);
    }

    public String getId() {
        return id;
    }

    public List<MessageExt> getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Message next() {
        return iterator.next();
    }

    @Override
    public void remove() {
        iterator.remove();
    }

    @Override
    public String toString() {
        return data.toString();
    }
}
