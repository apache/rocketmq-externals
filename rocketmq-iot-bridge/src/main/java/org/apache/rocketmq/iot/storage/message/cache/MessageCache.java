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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.iot.storage.message.cache;

import org.apache.rocketmq.iot.common.data.Message;

public interface MessageCache {

    /**
     * get message by MessageId
     * @param topic the topic to which the message belongs
     * @param id the identifier of a message
     * @return the message whose id equals given id
     */
    Message get(String topic, String id);

    /**
     * get latest message of the topic
     * @param topic
     * @return
     */
    Message get(String topic);

    /**
     * put message with key MessageId
     * @param topic topic of the message, key of the storage pair
     * @param message actual message, value of the storage pair
     */
    void put(String topic, Message message);

    /**
     * start the MessageCache
     */
    void start();

    /**
     * shutdown the MessageCache
     */
    void shutdown();
}
