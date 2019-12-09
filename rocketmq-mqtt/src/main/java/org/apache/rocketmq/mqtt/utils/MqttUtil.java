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

package org.apache.rocketmq.mqtt.utils;

import io.netty.handler.codec.mqtt.MqttQoS;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.rocketmq.common.annotation.ImportantField;
import org.apache.rocketmq.mqtt.common.MqttSubscriptionData;
import org.apache.rocketmq.mqtt.constant.MqttConstant;
import org.eclipse.paho.client.mqttv3.util.Strings;
import org.slf4j.Logger;

import static org.eclipse.paho.client.mqttv3.MqttTopic.MULTI_LEVEL_WILDCARD;
import static org.eclipse.paho.client.mqttv3.MqttTopic.MULTI_LEVEL_WILDCARD_PATTERN;
import static org.eclipse.paho.client.mqttv3.MqttTopic.SINGLE_LEVEL_WILDCARD;
import static org.eclipse.paho.client.mqttv3.MqttTopic.TOPIC_LEVEL_SEPARATOR;
import static org.eclipse.paho.client.mqttv3.MqttTopic.TOPIC_WILDCARDS;

public class MqttUtil {

    private static final int MIN_TOPIC_LEN = 1;
    private static final int MAX_TOPIC_LEN = 65535;
    private static final char NUL = '\u0000';

    public static String generateClientId() {
        return UUID.randomUUID().toString();
    }

    public static String getRootTopic(String topic) {
        return topic.split(MqttConstant.SUBSCRIPTION_SEPARATOR)[0];
    }

    public static int actualQos(int qos) {
        return Math.min(MqttConstant.MAX_SUPPORTED_QOS, qos);
    }

    public static boolean isQosLegal(MqttQoS qos) {
        if (!qos.equals(MqttQoS.AT_LEAST_ONCE) && !qos.equals(MqttQoS.AT_MOST_ONCE) && !qos.equals(MqttQoS.EXACTLY_ONCE)) {
            return false;
        }
        return true;
    }

    public static boolean isMatch(String topicFiter, String topic) {
        if (!topicFiter.contains(MqttConstant.SUBSCRIPTION_FLAG_PLUS) && !topicFiter.contains(MqttConstant.SUBSCRIPTION_FLAG_SHARP)) {
            return topicFiter.equals(topic);
        }
        String[] filterTopics = topicFiter.split(MqttConstant.SUBSCRIPTION_SEPARATOR);
        String[] actualTopics = topic.split(MqttConstant.SUBSCRIPTION_SEPARATOR);

        int i = 0;
        for (; i < filterTopics.length && i < actualTopics.length; i++) {
            if (MqttConstant.SUBSCRIPTION_FLAG_PLUS.equals(filterTopics[i])) {
                continue;
            }
            if (MqttConstant.SUBSCRIPTION_FLAG_SHARP.equals(filterTopics[i])) {
                return true;
            }
            if (!filterTopics[i].equals(actualTopics[i])) {
                return false;
            }
        }
        return i == actualTopics.length;
    }

    /**
     * @param topicFilters 订阅主题集合
     * @param topic 具体的主题
     * @return true: 具体的主题topic能与订阅主题集合topicFilters匹配；false: 不匹配
     */
    public static boolean isContain(Set<String> topicFilters, String topic) {
        for (String topicFilter : topicFilters) {
            if (isMatch(topicFilter, topic))
                return true;
        }
        return false;
    }

    public static void validate(String topicString, boolean wildcardAllowed)
        throws IllegalArgumentException {
        int topicLen = 0;
        try {
            topicLen = topicString.getBytes("UTF-8").length;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e.getMessage());
        }

        // Spec: length check
        // - All Topic Names and Topic Filters MUST be at least one character
        // long
        // - Topic Names and Topic Filters are UTF-8 encoded strings, they MUST
        // NOT encode to more than 65535 bytes
        if (topicLen < MIN_TOPIC_LEN || topicLen > MAX_TOPIC_LEN) {
            throw new IllegalArgumentException(String.format("Invalid topic length, should be in range[%d, %d]!",
                new Object[] {new Integer(MIN_TOPIC_LEN), new Integer(MAX_TOPIC_LEN)}));
        }

        // *******************************************************************************
        // 1) This is a topic filter string that can contain wildcard characters
        // *******************************************************************************
        if (wildcardAllowed) {
            // Only # or +
            if (Strings.equalsAny(topicString, new String[] {MULTI_LEVEL_WILDCARD, SINGLE_LEVEL_WILDCARD})) {
                return;
            }

            // 1) Check multi-level wildcard
            // Rule:
            // The multi-level wildcard can be specified only on its own or next
            // to the topic level separator character.

            // - Can only contains one multi-level wildcard character
            // - The multi-level wildcard must be the last character used within
            // the topic tree
            if (Strings.countMatches(topicString, MULTI_LEVEL_WILDCARD) > 1
                || (topicString.contains(MULTI_LEVEL_WILDCARD) && !topicString
                .endsWith(MULTI_LEVEL_WILDCARD_PATTERN))) {
                throw new IllegalArgumentException(
                    "Invalid usage of multi-level wildcard in topic string: "
                        + topicString);
            }

            // 2) Check single-level wildcard
            // Rule:
            // The single-level wildcard can be used at any level in the topic
            // tree, and in conjunction with the
            // multilevel wildcard. It must be used next to the topic level
            // separator, except when it is specified on
            // its own.
            validateSingleLevelWildcard(topicString);

            return;
        }

        // *******************************************************************************
        // 2) This is a topic name string that MUST NOT contains any wildcard characters
        // *******************************************************************************
        if (Strings.containsAny(topicString, TOPIC_WILDCARDS)) {
            throw new IllegalArgumentException(
                "The topic name MUST NOT contain any wildcard characters (#+)");
        }
    }

    private static void validateSingleLevelWildcard(String topicString) {
        char singleLevelWildcardChar = SINGLE_LEVEL_WILDCARD.charAt(0);
        char topicLevelSeparatorChar = TOPIC_LEVEL_SEPARATOR.charAt(0);

        char[] chars = topicString.toCharArray();
        int length = chars.length;
        char prev = NUL, next = NUL;
        for (int i = 0; i < length; i++) {
            prev = (i - 1 >= 0) ? chars[i - 1] : NUL;
            next = (i + 1 < length) ? chars[i + 1] : NUL;

            if (chars[i] == singleLevelWildcardChar) {
                // prev and next can be only '/' or none
                if (prev != topicLevelSeparatorChar && prev != NUL || next != topicLevelSeparatorChar && next != NUL) {
                    throw new IllegalArgumentException(String.format(
                        "Invalid usage of single-level wildcard in topic string '%s'!",
                        new Object[] {topicString}));
                }
            }
        }
    }

    /**
     * 检查该Client是否订阅了该publishTopic
     *
     * @param publishTopic 发布主题
     * @param subscriptions subscriber的订阅数据
     * @return 是否订阅了该publishTopic
     */
    public static boolean isSubscriptionMatch(String publishTopic, Map<String, MqttSubscriptionData> subscriptions) {
        Set<String> topicFilters = subscriptions.keySet();
        for (String topicFilter : topicFilters) {
            if (MqttUtil.isMatch(topicFilter, publishTopic)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算subscriber对发布主题的最大订阅qos
     *
     * @param publishTopic 发布主题
     * @param subscriptions subscriber的订阅数据
     * @return maxRequestedQos subscriber对发布主题的最大订阅qos
     */
    public static Integer calcMaxRequiredQos(String publishTopic, Map<String, MqttSubscriptionData> subscriptions) {
        Integer maxRequestedQos = -1;
        Iterator<Map.Entry<String, MqttSubscriptionData>> iterator = subscriptions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, MqttSubscriptionData> entry = iterator.next();
            final String topicFilter = entry.getKey();
            if (MqttUtil.isMatch(topicFilter, publishTopic)) {
                MqttSubscriptionData mqttSubscriptionData = entry.getValue();
                maxRequestedQos = mqttSubscriptionData.getQos() > maxRequestedQos ? mqttSubscriptionData.getQos() : maxRequestedQos;
            }
        }
        return maxRequestedQos;
    }

    public static void printObjectProperties(Logger logger, Object object, boolean onlyImportantField) {
        Field[] fields = object.getClass().getDeclaredFields();
        Field[] var4 = fields;
        int var5 = fields.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            Field field = var4[var6];
            if (!Modifier.isStatic(field.getModifiers())) {
                String name = field.getName();
                if (!name.startsWith("this")) {
                    Object value = null;

                    try {
                        field.setAccessible(true);
                        value = field.get(object);
                        if (null == value) {
                            value = "";
                        }
                    } catch (IllegalAccessException var11) {
                        logger.error("Failed to obtain object properties", var11);
                    }

                    if (onlyImportantField) {
                        Annotation annotation = field.getAnnotation(ImportantField.class);
                        if (null == annotation) {
                            continue;
                        }
                    }

                    if (logger != null) {
                        logger.info(name + "=" + value);
                    }
                }
            }
        }

    }
}
