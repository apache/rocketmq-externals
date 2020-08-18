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

import com.alibaba.fastjson.JSON;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.config.ConfigReply;
import io.grpc.config.ConfigServiceGrpc;
import io.grpc.config.CreateConnectorRequest;
import io.grpc.config.StopAllRequest;
import io.grpc.config.StopConnectorRequest;
import io.grpc.stub.StreamObserver;
import org.apache.rocketmq.connect.runtime.common.ConnectKeyValue;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.config.RPCConfigDefine;
import org.apache.rocketmq.connect.runtime.service.ConfigManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class ConfigServer {
    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    private Server server;

    private final ConfigManagementService configManagementService;

    public ConfigServer(ConfigManagementService configManagementService) {
        this.configManagementService = configManagementService;
    }

    public void start() throws IOException {
        server = ServerBuilder.forPort(RPCConfigDefine.PORT)
                .addService(new ConfigServiceImpl(configManagementService))
                .build()
                .start();
        log.info("PRC Server started, listening on "+ RPCConfigDefine.PORT);

        Runtime.getRuntime().addShutdownHook(new Thread(){

            @Override
            public void run(){

                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                ConfigServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null){
            server.awaitTermination();
        }
    }


    private static class ConfigServiceImpl extends ConfigServiceGrpc.ConfigServiceImplBase {

        private final ConfigManagementService configManagementService;

        public ConfigServiceImpl(ConfigManagementService configManagementService) {
            this.configManagementService = configManagementService;
        }

        @Override
        public void stopConnector(StopConnectorRequest request, StreamObserver<ConfigReply> responseObserver) {
            String connectorName = request.getConnectorName();
            ConfigReply reply;
            try {
                configManagementService.removeConnectorConfig(connectorName);
                reply = ConfigReply.newBuilder().setResult("success").build();
            } catch (Exception e) {
                reply = ConfigReply.newBuilder().setResult("failed").build();
            }
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
            log.info("Get stop {} connector request from slave", connectorName);
        }

        @Override
        public void stopAll(StopAllRequest request, StreamObserver<ConfigReply> responseObserver) {
            ConfigReply reply;
            try {
                Map<String, ConnectKeyValue> connectorConfigs = configManagementService.getConnectorConfigs();
                for (String connector : connectorConfigs.keySet()) {
                    configManagementService.removeConnectorConfig(connector);
                }
                reply = ConfigReply.newBuilder().setResult("success").build();
            } catch (Exception e) {
                reply = ConfigReply.newBuilder().setResult("failed").build();
            }
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
            log.info("Get stop all connector request from slave");
        }

        @Override
        public void createConnector(CreateConnectorRequest request, StreamObserver<ConfigReply> responseObserver) {
            String connectorName = request.getConnectorName();
            ConfigReply reply;
            String config = request.getConfig();
            if (config == null) {
                reply = ConfigReply.newBuilder().setResult("failed! query param 'config' is required ").build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            }
            log.info("Get create connector request from slave, config: {}", config);
            Map keyValue = JSON.parseObject(config, Map.class);
            ConnectKeyValue configs = new ConnectKeyValue();
            for (Object key : keyValue.keySet()) {
                configs.put((String) key, keyValue.get(key).toString());
            }
            try {

                String result = configManagementService.putConnectorConfig(connectorName, configs);
                if (result != null && result.length() > 0) {
                    reply = ConfigReply.newBuilder().setResult(result).build();
                } else {
                    reply = ConfigReply.newBuilder().setResult("success").build();
                }
            } catch (Exception e) {
                log.error("Handle createConnector error .", e);
                reply = ConfigReply.newBuilder().setResult("failed").build();
            }
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
}
