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

package org.apache.rocketmq.jms.support;

import java.util.UUID;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.jms.Queue;
import javax.jms.Topic;

public class JmsHelper {

    public static final boolean SKIP_SET_EXCEPTION
        = Boolean.parseBoolean(System.getProperty("skip.set.exception", "false"));

    public static String getTopicName(Destination destination) {
        try {
            String topicName;
            if (destination instanceof Topic) {
                topicName = ((Topic) destination).getTopicName();
            }
            else if (destination instanceof Queue) {
                topicName = ((Queue) destination).getQueueName();
            }
            else {
                throw new JMSException(String.format("Unsupported Destination type:", destination.getClass()));
            }
            return topicName;
        }
        catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    public static void handleUnSupportedException() {
        if (!SKIP_SET_EXCEPTION) {
            throw new UnsupportedOperationException("Operation unsupported! If you want to skip this Exception," +
                " use '-Dskip.set.exception=true' in JVM options.");
        }
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }
}
