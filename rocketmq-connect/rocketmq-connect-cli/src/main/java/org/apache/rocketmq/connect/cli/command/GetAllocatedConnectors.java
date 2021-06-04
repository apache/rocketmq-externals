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

package org.apache.rocketmq.connect.cli.command;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.rocketmq.connect.cli.commom.CLIConfigDefine;
import org.apache.rocketmq.connect.cli.commom.Config;
import org.apache.rocketmq.connect.cli.commom.ConnectKeyValue;
import org.apache.rocketmq.connect.cli.utils.RestSender;
import org.apache.rocketmq.tools.command.SubCommandException;

import java.net.URL;
import java.util.Map;

import static org.apache.rocketmq.connect.cli.utils.FileAndPropertyUtil.stringToKeyValue;

public class GetAllocatedConnectors implements SubCommand {

    private final Config config;

    public GetAllocatedConnectors(Config config) {
        this.config = config;
    }

    @Override
    public String commandName() {
        return "getAllocatedConnectors";
    }

    @Override
    public String commandDesc() {
        return "Get allocated connectors information";
    }

    @Override
    public Options buildCommandlineOptions(Options options) {
        return options;
    }

    @Override
    public void execute(CommandLine commandLine, Options options) throws SubCommandException {
        try {
            String request = "/getAllocatedConnectors";
            URL url = new URL(CLIConfigDefine.PROTOCOL, config.getHttpAddr(), config.getHttpPort(), request);
            String result = new RestSender().sendHttpRequest(url.toString(), "");
            showConnectorInfo(result);
        } catch (Exception e) {
            throw new SubCommandException(this.getClass().getSimpleName() + " command failed", e);
        }
    }

    private void showConnectorInfo(String result) {
        Map<String, JSONObject> connectMap = JSON.parseObject(result, Map.class);
        System.out.printf("%-25s %-15s %-17s %-25s%n", "connectorName", "status", "topic", "update-timestamp");
        for (Map.Entry<String, JSONObject> entry : connectMap.entrySet()) {
            String json = entry.getValue().getString("properties");
            ConnectKeyValue keyValue = stringToKeyValue(json);
            String connectorName = entry.getKey();
            String topic;
            if (keyValue.getString("topic") != null) {
                topic = keyValue.getString("topic");
            } else {
                topic = keyValue.getString("topicNames");
            }
            String updateTimestamp = keyValue.getString("update-timestamp");
            System.out.printf("%-25s %-15s %-17s %-25s%n", connectorName, "RUNNING", topic, updateTimestamp);
        }
    }
}
