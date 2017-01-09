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

package org.apache.rocketmq.jms.domain;

public interface JmsBaseConstant {
    //------------------------JMS message header constant---------------------------------
    String jmsDestination = "jmsDestination";
    String jmsDeliveryMode = "jmsDeliveryMode";
    String jmsExpiration = "jmsExpiration";
    String jmsDeliveryTime = "jmsDeliveryTime";
    String jmsPriority = "jmsPriority";
    String jmsMessageID = "jmsMessageID";
    String jmsTimestamp = "jmsTimestamp";
    String jmsCorrelationID = "jmsCorrelationID";
    String jmsReplyTo = "jmsReplyTo";
    String jmsType = "jmsType";
    String jmsRedelivered = "jmsRedelivered";

    //-------------------------JMS defined properties constant----------------------------
    /**
     * The identity of the user sending the Send message
     */
    String jmsXUserID = "jmsXUserID";
    /**
     * The identity of the application Send sending the message
     */
    String jmsXAppID = "jmsXAppID";
    /**
     * The number of message delivery Receive attempts
     */
    String jmsXDeliveryCount = "jmsXDeliveryCount";
    /**
     * The identity of the message group this message is part of
     */
    String jmsXGroupID = "jmsXGroupID";
    /**
     * The sequence number of this message within the group; the first message is 1, the second 2,...
     */
    String jmsXGroupSeq = "jmsXGroupSeq";
    /**
     * The transaction identifier of the Send transaction within which this message was produced
     */
    String jmsXProducerTXID = "jmsXProducerTXID";
    /**
     * The transaction identifier of the Receive transaction within which this message was consumed
     */
    String jmsXConsumerTXID = "jmsXConsumerTXID";

    /**
     * The time JMS delivered the Receive message to the consumer
     */
    String jmsXRcvTimestamp = "jmsXRcvTimestamp";
    /**
     * Assume there exists a message warehouse that contains a separate copy of each message sent to each consumer and
     * that these copies exist from the time the original message was sent. Each copy’s state is one of: 1(waiting),
     * 2(ready), 3(expired) or 4(retained) Since state is of no interest to producers and consumers it is not provided
     * to either. It is only of relevance to messages looked up in a warehouse and JMS provides no API for this.
     */
    String jmsXState = "jmsXState";

    //---------------------------JMS Headers' value constant---------------------------
    /**
     * Default time to live
     */
    long defaultTimeToLive = 3 * 24 * 60 * 60 * 1000;

    /**
     * Default Jms Type
     */
    String defaultJmsType = "rocketmq";
}
