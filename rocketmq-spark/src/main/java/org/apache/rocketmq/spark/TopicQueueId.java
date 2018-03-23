/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.spark;
import java.io.Serializable;

public final class TopicQueueId implements Serializable {

    private int hash = 0;
    private final int queueId;
    private final String topic;

    public TopicQueueId(String topic, int queueId) {
        this.queueId = queueId;
        this.topic = topic;
    }

    public int queueId() {
        return queueId;
    }

    public String topic() {
        return topic;
    }

    @Override
    public int hashCode() {
        if (hash != 0) {
            return hash;
        }
        final int prime = 31;
        int result = 1;
        result = prime * result + queueId;
        result = prime * result + ((topic == null) ? 0 : topic.hashCode());
        this.hash = result;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TopicQueueId other = (TopicQueueId) obj;
        if (queueId != other.queueId) {
            return false;
        }
        if (topic == null) {
            if (other.topic != null) {
                return false;
            }
        } else {
            if (!topic.equals(other.topic)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected TopicQueueId clone() throws CloneNotSupportedException {
        return new TopicQueueId(this.topic, this.queueId);
    }

    @Override
    public String toString() {
        return topic + "-" + queueId;
    }

}