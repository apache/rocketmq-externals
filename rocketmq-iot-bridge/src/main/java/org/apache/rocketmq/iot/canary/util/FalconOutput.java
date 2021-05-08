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

package org.apache.rocketmq.iot.canary.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xiaomi.infra.galaxy.lcs.common.tools.FaclonOutput;
import java.lang.reflect.Method;
import org.apache.rocketmq.iot.canary.config.CanaryConfig;

public class FalconOutput extends FaclonOutput {
    private int intervalMillis;
    private String endpoint;
    private String clusterName;
    private double rate;
    private String dataType;
    private String falconMetric;

    public FalconOutput(CanaryConfig canaryConfig) {
        super(canaryConfig.getFalconUrl(), 0);
        this.intervalMillis = canaryConfig.getFalconIntervalMillis();
        this.endpoint = canaryConfig.getEndpoint();
        this.clusterName = canaryConfig.getClusterName();
    }

    public void pushFalconData(double rate, String dataType, String metric) {
        this.rate = rate;
        this.dataType = dataType;
        this.falconMetric = metric;
        this.startPushFaclonData();
    }

    @Override
    public void startPushFaclonData() {
        try {
            Method method = this.getClass().getSuperclass().getDeclaredMethod("pushFaclonData");
            method.setAccessible(true);
            method.invoke(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getFaclonData() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("endpoint", endpoint);
        jsonObject.addProperty("metric", falconMetric);
        jsonObject.addProperty("value", rate);
        jsonObject.addProperty("timestamp", System.currentTimeMillis() / 1000);
        jsonObject.addProperty("step", intervalMillis / 1000);
        jsonObject.addProperty("counterType", "GAUGE");
        jsonObject.addProperty("tags", "cluster=" + clusterName + "," + "type=" + dataType);
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(jsonObject);
        return jsonArray.toString();
    }
}
