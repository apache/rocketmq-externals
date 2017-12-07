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

package org.apache.rocketmq.redis.replicator.conf;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static org.apache.rocketmq.redis.replicator.conf.ReplicatorConstants.CONFIG_FILE;
import static org.apache.rocketmq.redis.replicator.conf.ReplicatorConstants.CONFIG_FILE_SYS_PROP_NAME;

public class Configure {

    private final Properties properties;

    public Configure() {
        this.properties = new Properties();
        try {
            String path = System.getProperty(CONFIG_FILE_SYS_PROP_NAME);
            if (path != null && path.trim().length() != 0) {
                properties.load(new FileInputStream(path));
            } else {
                path = CONFIG_FILE;
                properties.load(Configure.class.getResourceAsStream(path));
            }
        } catch (IOException e) {
            throw new ConfigureException(e);
        }
    }

    public Configure(Properties properties) {
        this();
        this.properties.putAll(properties);
    }

    public String getString(String key) {
        return getString(key, null, false);
    }

    public Integer getInt(String key) {
        return getInt(key, null, false);
    }

    public Boolean getBool(String key) {
        return getBool(key, null, false);
    }

    public String getString(String key, String value, boolean optional) {
        String v = System.getProperty(key);
        if (v == null && (v = properties.getProperty(key)) == null)
            v = value;
        if (v == null && !optional) {
            throw new ConfigureException(String.format("Not found the config[key=%s]", key));
        }
        return v;
    }

    public Integer getInt(String key, Integer value, boolean optional) {
        String v = getString(key, value == null ? null : value.toString(), optional);
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new ConfigureException(String.format("Invalid config[key=%s]", key));
        }
    }

    public Boolean getBool(String key, Boolean value, boolean optional) {
        String v = getString(key, value == null ? null : value.toString(), optional);
        if (v == null)
            return value;
        if (v.equals("yes") || v.equals("true"))
            return Boolean.TRUE;
        if (v.equals("no") || v.equals("false"))
            return Boolean.FALSE;
        throw new ConfigureException(String.format("Invalid config[key=%s]", key));
    }
}
