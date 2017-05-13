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

package org.apache.rocketmq.replicator.redis.conf;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.rocketmq.replicator.redis.conf.Constant.CONFIG_FILE;
import static org.apache.rocketmq.replicator.redis.conf.Constant.CONFIG_FILE_SYS_PROP_NAME;

public class Configure {

    private static final Logger logger = LoggerFactory.getLogger(Configure.class);

    private static Properties properties;

    static {
        properties = new Properties();
        String path = "";
        try {
            path = System.getProperty(CONFIG_FILE_SYS_PROP_NAME);
            if (StringUtils.isNotBlank(path)) {
                properties.load(new FileInputStream(path));
            }
            else {
                path = CONFIG_FILE;
                properties.load(Configure.class.getResourceAsStream(path));
            }
        }
        catch (IOException e) {
            logger.error("Fail to load config file[path={}],errorMsg:{}", path, ExceptionUtils.getStackTrace(e));
            throw new ConfigureException(e);
        }
    }

    public static void setProperties(Properties props) {
        properties = props;
    }

    public static void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public static String get(String key) {
        return get(key, true);
    }

    public static String get(String key, boolean mustExist) {
        String value = System.getProperty(key);
        if (value == null) {
            value = properties.getProperty(key);
        }

        if (value == null && mustExist) {
            throw new ConfigureException(String.format("Not found the config[key=%s]", key));
        }

        return value;
    }

    public static <T> T get(String key, Class<T> claz) {
        String value = get(key);
        if (claz == Integer.class) {
            return (T)new Integer(Integer.parseInt(value));
        }
        else {
            throw new UnsupportedOperationException(String.format("Unsupported type[%s]", claz.getSimpleName()));
        }
    }
}
