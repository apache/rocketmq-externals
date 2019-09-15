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

package org.apache.rocketmq.connect.jdbc;

import io.openmessaging.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Config {
    private static final Logger LOG = LoggerFactory.getLogger(Config.class);

    /* Database Connection Config */
    public String jdbcUrl = "localhost:3306";
    public String jdbcUsername = "root";
    public String jdbcPassword = "199812160";
    public String rocketmqTopic;
    public String jdbcBackoff;
    public String jdbcAttempts;
    public String catalogPattern = null;
    public List tableWhitelist;
    public List tableBlacklist;
    public String schemaPattern = null;
    public boolean numericPrecisionMapping = false;
    public String bumericMapping = null;
    public String dialectName = "";

    /* Mode Config */
    public String mode = "";
    public String incrementingColumnName = "";
    public String query = "";
    public String timestampColmnName = "";
    public boolean validateNonNull = true;

    /*Connector config*/
    public String tableTypes = "table";
    public long pollInterval = 5000;
    public int batchMaxRows = 100;
    public long tablePollInterval = 60000;
    public long timestampDelayInterval = 0;
    public String dbTimezone = "UTC";
    public String queueName;

    private Logger log = LoggerFactory.getLogger(Config.class);
    public static final Set<String> REQUEST_CONFIG = new HashSet<String>() {
        {
            add("jdbcUrl");
            add("jdbcUsername");
            add("jdbcPassword");
            add("mode");
            add("rocketmqTopic");
        }
    };


    public void load(KeyValue props) {
        log.info("Config.load.start");
        properties2Object(props, this);
    }

    private void properties2Object(final KeyValue p, final Object object) {
        Method[] methods = object.getClass().getMethods();
        for (Method method : methods) {
            String mn = method.getName();
            if (mn.startsWith("set")) {
                try {
                    String tmp = mn.substring(4);
                    String first = mn.substring(3, 4);

                    String key = first.toLowerCase() + tmp;
                    String property = p.getString(key);
                    if (property != null) {
                        Class<?>[] pt = method.getParameterTypes();
                        if (pt != null && pt.length > 0) {
                            String cn = pt[0].getSimpleName();
                            Object arg = null;
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

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getJdbcUsername() {
        return jdbcUsername;
    }

    public void setJdbcUsername(String jdbcUsername) {
        this.jdbcUsername = jdbcUsername;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public void setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    public String getRocketmqTopic() {
        return rocketmqTopic;
    }

    public void setRocketmqTopic(String rocketmqTopic) {
        this.rocketmqTopic = rocketmqTopic;
    }

    public String getJdbcBackoff() {
        return jdbcBackoff;
    }

    public void setJdbcBackoff(String jdbcBackoff) {
        this.jdbcBackoff = jdbcBackoff;
    }

    public String getJdbcAttempts() {
        return jdbcAttempts;
    }

    public void setJdbcAttempts(String jdbcAttempts) {
        this.jdbcAttempts = jdbcAttempts;
    }

    public String getCatalogPattern() {
        return catalogPattern;
    }

    public void setCatalogPattern(String catalogPattern) {
        this.catalogPattern = catalogPattern;
    }

    public List getTableWhitelist() {
        return tableWhitelist;
    }

    public void setTableWhitelist(List tableWhitelist) {
        this.tableWhitelist = tableWhitelist;
    }

    public List getTableBlacklist() {
        return tableBlacklist;
    }

    public void setTableBlacklist(List tableBlacklist) {
        this.tableBlacklist = tableBlacklist;
    }

    public String getSchemaPattern() {
        return schemaPattern;
    }

    public void setSchemaPattern(String schemaPattern) {
        this.schemaPattern = schemaPattern;
    }

    public boolean isNumericPrecisionMapping() {
        return numericPrecisionMapping;
    }

    public void setNumericPrecisionMapping(boolean numericPrecisionMapping) {
        this.numericPrecisionMapping = numericPrecisionMapping;
    }

    public String getBumericMapping() {
        return bumericMapping;
    }

    public void setBumericMapping(String bumericMapping) {
        this.bumericMapping = bumericMapping;
    }

    public String getDialectName() {
        return dialectName;
    }

    public void setDialectName(String dialectName) {
        this.dialectName = dialectName;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getIncrementingColumnName() {
        return incrementingColumnName;
    }

    public void setIncrementingColumnName(String incrementingColumnName) {
        this.incrementingColumnName = incrementingColumnName;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getTimestampColmnName() {
        return timestampColmnName;
    }

    public void setTimestampColmnName(String timestampColmnName) {
        this.timestampColmnName = timestampColmnName;
    }

    public boolean isValidateNonNull() {
        return validateNonNull;
    }

    public void setValidateNonNull(boolean validateNonNull) {
        this.validateNonNull = validateNonNull;
    }

    public String getTableTypes() {
        return tableTypes;
    }

    public void setTableTypes(String tableTypes) {
        this.tableTypes = tableTypes;
    }

    public long getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
    }

    public int getBatchMaxRows() {
        return batchMaxRows;
    }

    public void setBatchMaxRows(int batchMaxRows) {
        this.batchMaxRows = batchMaxRows;
    }

    public long getTablePollInterval() {
        return tablePollInterval;
    }

    public void setTablePollInterval(long tablePollInterval) {
        this.tablePollInterval = tablePollInterval;
    }

    public long getTimestampDelayInterval() {
        return timestampDelayInterval;
    }

    public void setTimestampDelayInterval(long timestampDelayInterval) {
        this.timestampDelayInterval = timestampDelayInterval;
    }

    public String getDbTimezone() {
        return dbTimezone;
    }

    public void setDbTimezone(String dbTimezone) {
        this.dbTimezone = dbTimezone;
    }
}