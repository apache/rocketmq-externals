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

package org.apache.rocketmq.iot.canary;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.rocketmq.iot.canary.config.CanaryConfig;
import org.apache.rocketmq.srvutil.ServerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQTTBridgeCanary {
    private Logger logger = LoggerFactory.getLogger(MQTTBridgeCanary.class);
    private CanaryConfig canaryConfig;
    private CanaryController controller;

    public MQTTBridgeCanary(String[] args) {
        initConfig(args);
        this.controller = new CanaryController(canaryConfig);
    }

    private void initConfig(String[] args) {
        Properties properties = loadConfigProperties(args);
        if (properties == null) {
            logger.info("load canary config properties file failed, process exit.");
            System.exit(-1);
        }
        logger.info("load canary config properties file success, properties:{}", properties);

        this.canaryConfig = new CanaryConfig(properties);
        logger.info("init canary config:{}", canaryConfig);
    }

    private Properties loadConfigProperties(String[] args) {
        CommandLine commandLine = ServerUtil.parseCmdLine("rmq-mqtt-canary", args,
            buildCommandlineOptions(new Options()), new PosixParser());
        if (null == commandLine) {
            logger.error("load config properties file failed, lack properties file path.");
            System.exit(-1);
        }

        try {
            if (commandLine.hasOption('c')) {
                String file = commandLine.getOptionValue('c');
                if (file != null) {
                    Properties properties = new Properties();
                    InputStream in = new BufferedInputStream(new FileInputStream(file));
                    properties.load(in);
                    logger.info("load config properties file OK, {}", file);
                    in.close();
                    return properties;
                }
            }
        } catch (Exception e) {
            logger.error("load config properties file exception.", e);
        }
        return null;
    }

    private Options buildCommandlineOptions(final Options options) {
        Option opt = new Option("c", "configFile", true, "MQTT broker config properties file");
        opt.setRequired(false);
        options.addOption(opt);
        return options;
    }

    public void start() {
        controller.start();
        logger.info("start canaryController.");
    }

    public static void main(String[] args) {
        MQTTBridgeCanary canaryStartup = new MQTTBridgeCanary(args);
        canaryStartup.start();
    }
}
