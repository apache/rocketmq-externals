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
package org.apache.rocketmq.hbase.source;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Config is a convenience class that maintains all configuration properties that are needed for the application.
 */
public class Config {

    private static final String TOPICS_SEPARATOR = ",";

    private String nameserver = "localhost:9876";

    private String consumerGroup = "HBASE_CONSUMER_GROUP";

    private String messageModel = "BROADCASTING";

    private String topics;

    private String zookeeperAddress = "localhost";

    private int zookeeperPort = 2181;

    private long pullInterval = 1000;

    private int batchSize = 32;

    /**
     * Loads all properties from the configuration file.
     *
     * @throws IOException
     */
    public void load() throws IOException {
        InputStream in = Config.class.getClassLoader().getResourceAsStream("rocketmq_hbase.conf");
        Properties properties = new Properties();
        properties.load(in);

        properties2Object(properties, this);
    }

    /**
     * Populate class fields with properties.
     *
     * @param p the properties instance
     * @param object this class instance
     */
    private void properties2Object(final Properties p, final Object object) {
        Method[] methods = object.getClass().getMethods();
        for (Method method : methods) {
            String mn = method.getName();
            if (mn.startsWith("set")) {
                try {
                    String tmp = mn.substring(4);
                    String first = mn.substring(3, 4);

                    String key = first.toLowerCase() + tmp;
                    String property = p.getProperty(key);
                    if (property != null) {
                        Class<?>[] pt = method.getParameterTypes();
                        if (pt != null && pt.length > 0) {
                            String cn = pt[0].getSimpleName();
                            Object arg;
                            if (cn.equals("int") || cn.equals("Integer")) {
                                arg = Integer.parseInt(property);
                            } else if (cn.equals("long") || cn.equals("Long")) {
                                arg = Long.parseLong(property);
                            } else if (cn.equals("double") || cn.equals("Double")) {
                                arg = Double.parseDouble(property);
                            } else if (cn.equals("boolean") || cn.equals("Boolean")) {
                                arg = Boolean.parseBoolean(property);
                            } else if (cn.equals("float") || cn.equals("Float")) {
                                arg = Float.parseFloat(property);
                            } else if (cn.equals("String")) {
                                arg = property;
                            } else {
                                continue;
                            }
                            method.invoke(object, arg);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public String getNameserver() {
        return nameserver;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public String getMessageModel() {
        return messageModel;
    }

    public Set<String> getTopics() {
        final String[] topicsArr = topics.split(TOPICS_SEPARATOR);
        final Set<String> topicsSet = new HashSet<>();
        Collections.addAll(topicsSet, topicsArr);
        return topicsSet;
    }

    public String getZookeeperAddress() {
        return zookeeperAddress;
    }

    public int getZookeeperPort() {
        return zookeeperPort;
    }

    public long getPullInterval() {
        return pullInterval;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setNameserver(String nameserver) {
        this.nameserver = nameserver;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public void setMessageModel(String messageModel) {
        this.messageModel = messageModel;
    }

    public void setTopics(String topics) {
        this.topics = topics;
    }

    public void setZookeeperAddress(String zookeeperAddress) {
        this.zookeeperAddress = zookeeperAddress;
    }

    public void setZookeeperPort(int zookeeperPort) {
        this.zookeeperPort = zookeeperPort;
    }

    public void setPullInterval(long pullInterval) {
        this.pullInterval = pullInterval;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
