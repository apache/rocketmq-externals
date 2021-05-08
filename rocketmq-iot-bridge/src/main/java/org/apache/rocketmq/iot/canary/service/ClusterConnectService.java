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

package org.apache.rocketmq.iot.canary.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.util.Properties;
import org.apache.rocketmq.iot.canary.config.CanaryConfig;
import org.apache.rocketmq.iot.canary.util.FalconOutput;
import org.apache.rocketmq.iot.common.util.HttpAPIClient;
import org.apache.rocketmq.iot.rest.common.ConnectionInfo;
import org.apache.rocketmq.iot.rest.common.ContextResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterConnectService implements FalconDataService {
    private Logger logger = LoggerFactory.getLogger(ClusterConnectService.class);

    private CanaryConfig canaryConfig;
    private FalconOutput falconOutput;
    private String metric;
    private String connectNumberURI;
    private Gson gson;

    public static void main(String[] args) {
        Properties properties = new Properties();
        ClusterConnectService connectNumberService = new ClusterConnectService(new CanaryConfig(properties));
        connectNumberService.report();
    }

    public ClusterConnectService(CanaryConfig canaryConfig) {
        this.canaryConfig = canaryConfig;
        this.falconOutput = new FalconOutput(canaryConfig);
        this.metric = canaryConfig.getMetricConnect();
        this.connectNumberURI = canaryConfig.getConnectNumberURI();
        this.gson = new GsonBuilder().create();
    }

    public int getConnectNumber() {
        try {
            String result = HttpAPIClient.executeHttpGet(connectNumberURI);
            if (result != null && !result.isEmpty() && result.contains("status")) {
                ContextResponse<ConnectionInfo> contextResponse = gson.fromJson(result,
                    new TypeToken<ContextResponse<ConnectionInfo>>() {
                    }.getType());
                if (contextResponse.getStatus() == 200) {
                    return contextResponse.getData().getTotalNum();
                } else {
                    logger.error("request http broker connection failed, response status:{}, url:{}, " +
                        "errorMsg:{}", contextResponse.getStatus(), connectNumberURI, contextResponse.getMsg());
                }
            }
        } catch (Exception e) {
            logger.error("request cluster http connection exception, url:{}:", connectNumberURI, e);
        }
        return 0;
    }

    @Override public void report() {
        int connectNumber = getConnectNumber();
        falconOutput.pushFalconData(connectNumber, "connectNumber", metric);
        logger.info("mqtt cluster connect number:" + connectNumber);
    }

    @Override public void shutdown() {

    }
}
