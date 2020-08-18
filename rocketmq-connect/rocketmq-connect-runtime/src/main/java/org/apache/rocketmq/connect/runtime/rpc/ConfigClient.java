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

package org.apache.rocketmq.connect.runtime.rpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.config.ConfigReply;
import io.grpc.config.ConfigServiceGrpc;
import io.grpc.config.CreateConnectorRequest;
import io.grpc.config.StopAllRequest;
import io.grpc.config.StopConnectorRequest;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;

public class ConfigClient {

    private final ManagedChannel channel;
    private final ConfigServiceGrpc.ConfigServiceBlockingStub configBlockingStub;
    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    public ConfigClient(String host, int port){
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        configBlockingStub = ConfigServiceGrpc.newBlockingStub(channel);
    }


    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }


    public String sendStopConnector(String connectorName) {
        StopConnectorRequest request = StopConnectorRequest.newBuilder().setConnectorName(connectorName).build();
        ConfigReply response;
        try {
            response = configBlockingStub.stopConnector(request);
        } catch (StatusRuntimeException e) {
            log.error("RPC failed: {}", e.getStatus());
            return "RPC failed";
        }
        log.info("send stop connector result:{}", response.getResult());
        return response.getResult();
    }

    public String sendStopAll() {
        StopAllRequest request = StopAllRequest.newBuilder().setRequest(true).build();
        ConfigReply response;
        try {
            response = configBlockingStub.stopAll(request);
        } catch (StatusRuntimeException e) {
            log.error("RPC failed: {}", e.getStatus());
            return "RPC failed";
        }
        log.info("send stop all result:{}", response.getResult());
        return response.getResult();
    }

    public String sendCreateConnector(String connectorName, String config) {
        CreateConnectorRequest request = CreateConnectorRequest.newBuilder().setConnectorName(connectorName).setConfig(config).build();
        ConfigReply response;
        try {
            response = configBlockingStub.createConnector(request);
        } catch (StatusRuntimeException e) {
            log.error("RPC failed: {}", e.getStatus());
            return "RPC failed";
        }
        log.info("Send create connector result:{}", response.getResult());
        return response.getResult();
    }

}
