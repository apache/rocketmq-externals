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
package org.apache.rocketmq.mqtt;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.mqtt.common.MqttBridgeConfig;
import org.apache.rocketmq.mqtt.config.MultiNettyServerConfig;
import org.apache.rocketmq.mqtt.persistence.redis.RedisConfig;
import org.apache.rocketmq.mqtt.utils.MqttUtil;
import org.apache.rocketmq.remoting.common.RemotingUtil;
import org.apache.rocketmq.remoting.common.TlsMode;
import org.apache.rocketmq.remoting.netty.*;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.srvutil.ServerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.rocketmq.remoting.netty.TlsSystemConfig.TLS_ENABLE;

public class MqttBridgeStartup {
    private static Logger log = LoggerFactory.getLogger(MqttBridgeStartup.class);
    public static Properties properties = null;
    public static CommandLine commandLine = null;
    public static String configFile = null;

    public static void main(String[] args) throws JoranException, CloneNotSupportedException {
        start(createMqttBridgeController(args));
    }

    public static MqttBridgeController start(MqttBridgeController controller) {
        try {
            controller.start();
            String tip = "The mqtt bridge[" + controller.getMqttBridgeConfig().getMqttBridgeName() + ", "
                    + controller.getMqttBridgeConfig().getMqttBridgeIP() + "] boot success. serializeType=" + RemotingCommand.getSerializeTypeConfigInThisServer();

            if (null != controller.getMqttBridgeConfig().getNamesrvAddr()) {
                tip += " and name server is " + controller.getMqttBridgeConfig().getNamesrvAddr();
            }
            log.info(tip);
            System.out.printf("%s%n", tip);
            return controller;
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
    }

    public static MqttBridgeController createMqttBridgeController(
            String[] args) throws JoranException, CloneNotSupportedException {

        if (null == System.getProperty(NettySystemConfig.COM_ROCKETMQ_REMOTING_SOCKET_SNDBUF_SIZE)) {
            NettySystemConfig.socketSndbufSize = 131072;
        }

        if (null == System.getProperty(NettySystemConfig.COM_ROCKETMQ_REMOTING_SOCKET_RCVBUF_SIZE)) {
            NettySystemConfig.socketRcvbufSize = 131072;
        }

        Options options = ServerUtil.buildCommandlineOptions(new Options());
        commandLine = ServerUtil.parseCmdLine("mqtt-bridge", args, buildCommandlineOptions(options),
                new PosixParser());
        if (null == commandLine) {
            System.exit(-1);
        }

        MqttBridgeConfig mqttBridgeConfig = new MqttBridgeConfig();
        final NettyClientConfig mqttClientConfig = new NettyClientConfig();
        final MultiNettyServerConfig multiNettyServerConfig = new MultiNettyServerConfig();
        multiNettyServerConfig.setListenPort(8005);
        multiNettyServerConfig.setMqttListenPort(1883);
        final RedisConfig redisConfig = new RedisConfig();
        mqttClientConfig.setUseTLS(Boolean.parseBoolean(System.getProperty(TLS_ENABLE,
                String.valueOf(TlsSystemConfig.tlsMode == TlsMode.ENFORCING))));

        try {
            if (commandLine.hasOption('c')) {
                String file = commandLine.getOptionValue('c');
                if (file != null) {
                    configFile = file;
                    InputStream in = new BufferedInputStream(new FileInputStream(file));
                    properties = new Properties();
                    properties.load(in);
                    MixAll.properties2Object(properties, mqttBridgeConfig);
                    MixAll.properties2Object(properties, mqttClientConfig);
                    MixAll.properties2Object(properties, multiNettyServerConfig);
                    MixAll.properties2Object(properties, redisConfig);

                    in.close();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        }
        mqttBridgeConfig.setMultiNettyServerConfig(multiNettyServerConfig);
        mqttBridgeConfig.setMqttClientConfig(mqttClientConfig);
        mqttBridgeConfig.setRedisConfig(redisConfig);

        MixAll.properties2Object(ServerUtil.commandLine2Properties(commandLine), mqttBridgeConfig);
        if (null == mqttBridgeConfig.getRocketmqHome()) {
            System.out.printf("Please set the %s variable in your environment to match the location of the RocketMQ installation", MixAll.ROCKETMQ_HOME_ENV);
            System.exit(-2);
        }

        String namesrvAddr = mqttBridgeConfig.getNamesrvAddr();
        if (null != namesrvAddr) {
            try {
                String[] addrArray = namesrvAddr.split(";");
                for (String addr : addrArray) {
                    RemotingUtil.string2SocketAddress(addr);
                }
            } catch (Exception e) {
                System.out.printf(
                        "The Name Server Address[%s] illegal, please set it as follows, \"127.0.0.1:9876;192.168.0.1:9876\"%n",
                        namesrvAddr);
                System.exit(-3);
            }
        }

        MqttUtil.printObjectProperties(log, mqttBridgeConfig, false);
        MqttUtil.printObjectProperties(log, mqttBridgeConfig.getMultiNettyServerConfig(), false);
        MqttUtil.printObjectProperties(log, mqttBridgeConfig.getMqttClientConfig(), false);
        MqttUtil.printObjectProperties(log, redisConfig, false);

        final MqttBridgeController mqttBridgeController = new MqttBridgeController(mqttBridgeConfig);

        boolean initResult = mqttBridgeController.initialize();
        if (!initResult) {
            mqttBridgeController.shutdown();
            System.exit(-4);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            private volatile boolean hasShutdown = false;
            private AtomicInteger shutdownTimes = new AtomicInteger(0);

            @Override
            public void run() {
                synchronized (this) {
                    log.info("Shutdown hook was invoked, {}", this.shutdownTimes.incrementAndGet());

                    if (!this.hasShutdown) {
                        this.hasShutdown = true;
                        long beginTime = System.currentTimeMillis();
                        mqttBridgeController.shutdown();
                        long consumingTimeTotal = System.currentTimeMillis() - beginTime;
                        log.info("Shutdown hook over, consuming total time(ms): {}", consumingTimeTotal);
                    }
                }
            }
        }, "ShutdownHook"));
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(lc);
        lc.reset();
        configurator.doConfigure(mqttBridgeConfig.getRocketmqHome() + "/conf/logback_mqtt.xml");

        return mqttBridgeController;
    }

    private static Options buildCommandlineOptions(final Options options) {
        Option opt = new Option("c", "configFile", true, "MqttBridge config properties file");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("p", "printConfigItem", false, "Print all config item");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option("m", "printImportantConfig", false, "Print important config item");
        opt.setRequired(false);
        options.addOption(opt);

        return options;
    }
}

