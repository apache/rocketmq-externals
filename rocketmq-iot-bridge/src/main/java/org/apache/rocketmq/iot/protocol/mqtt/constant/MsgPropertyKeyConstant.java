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

package org.apache.rocketmq.iot.protocol.mqtt.constant;

public class MsgPropertyKeyConstant {
    // is Dup
    public final static String MSG_IS_DUP = "isDup";
    // qos level
    public final static String MSG_QOS_LEVEL = "qosLevel";
    // isRetain
    public final static String MSG_IS_RETAIN = "isRetain";
    // isRetain
    public final static String MSG_REMAINING_LENGTH = "remainingLength";
    // packetId
    public final static String MSG_PACKET_ID = "packetId";
    // mqtt topic
    public final static String MQTT_TOPIC = "mqttTopic";
    // client Id
    public final static String CLIENT_ID = "clientId";
    // clean session flag
    public final static String CLEAN_SESSION_FLAG = "cleanSessionFlag";
}
