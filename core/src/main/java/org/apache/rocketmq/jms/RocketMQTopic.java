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

package org.apache.rocketmq.jms;

import com.google.common.base.Joiner;
import javax.jms.JMSException;
import javax.jms.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketMQTopic implements Topic {
    private static final Logger log = LoggerFactory.getLogger(RocketMQTopic.class);

    private String name;
    private String type;

    public RocketMQTopic(String name) {
        this.name = name;
        this.type = "*";
    }

    public RocketMQTopic(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getTopicName() throws JMSException {
        return this.name;
    }

    public String getTypeName() throws JMSException {
        return this.type;
    }

    public String setTypeName(String type) throws JMSException {
        return this.type = type;
    }

    public String toString() {
        String print = "";
        try {
            print = Joiner.on(":").join(this.getTopicName(), this.getTypeName());
        }
        catch (JMSException e) {
            log.error("Exception Caught in toString, e: {}", e);
        }
        return print;
    }
}
