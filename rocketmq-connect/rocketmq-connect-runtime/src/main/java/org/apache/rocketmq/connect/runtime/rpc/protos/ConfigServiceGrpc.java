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

package org.apache.rocketmq.connect.runtime.rpc.protos;


import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 *
 */
@javax.annotation.Generated(
        value = "by gRPC proto compiler (version 1.31.0)",
        comments = "Source: config.proto")
public final class ConfigServiceGrpc {

    private ConfigServiceGrpc() {
    }

    public static final String SERVICE_NAME = "connectorAndTaskConfig.ConfigService";

    // Static method descriptors that strictly reflect the proto.
    private static volatile io.grpc.MethodDescriptor<StopConnectorRequest, ConfigReply> getStopConnectorMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "StopConnector",
            requestType = StopConnectorRequest.class,
            responseType = ConfigReply.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<StopConnectorRequest,
            ConfigReply> getStopConnectorMethod() {
        io.grpc.MethodDescriptor<StopConnectorRequest, ConfigReply> getStopConnectorMethod;
        if ((getStopConnectorMethod = ConfigServiceGrpc.getStopConnectorMethod) == null) {
            synchronized (ConfigServiceGrpc.class) {
                if ((getStopConnectorMethod = ConfigServiceGrpc.getStopConnectorMethod) == null) {
                    ConfigServiceGrpc.getStopConnectorMethod = getStopConnectorMethod =
                            io.grpc.MethodDescriptor.<StopConnectorRequest, ConfigReply>newBuilder()
                                    .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                    .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StopConnector"))
                                    .setSampledToLocalTracing(true)
                                    .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            StopConnectorRequest.getDefaultInstance()))
                                    .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            ConfigReply.getDefaultInstance()))
                                    .setSchemaDescriptor(new ConfigServiceMethodDescriptorSupplier("StopConnector"))
                                    .build();
                }
            }
        }
        return getStopConnectorMethod;
    }

    private static volatile io.grpc.MethodDescriptor<StopAllRequest,
            ConfigReply> getStopAllMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "StopAll",
            requestType = StopAllRequest.class,
            responseType = ConfigReply.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<StopAllRequest,
            ConfigReply> getStopAllMethod() {
        io.grpc.MethodDescriptor<StopAllRequest, ConfigReply> getStopAllMethod;
        if ((getStopAllMethod = ConfigServiceGrpc.getStopAllMethod) == null) {
            synchronized (ConfigServiceGrpc.class) {
                if ((getStopAllMethod = ConfigServiceGrpc.getStopAllMethod) == null) {
                    ConfigServiceGrpc.getStopAllMethod = getStopAllMethod =
                            io.grpc.MethodDescriptor.<StopAllRequest, ConfigReply>newBuilder()
                                    .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                    .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StopAll"))
                                    .setSampledToLocalTracing(true)
                                    .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            StopAllRequest.getDefaultInstance()))
                                    .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            ConfigReply.getDefaultInstance()))
                                    .setSchemaDescriptor(new ConfigServiceMethodDescriptorSupplier("StopAll"))
                                    .build();
                }
            }
        }
        return getStopAllMethod;
    }

    private static volatile io.grpc.MethodDescriptor<CreateConnectorRequest,
            ConfigReply> getCreateConnectorMethod;

    @io.grpc.stub.annotations.RpcMethod(
            fullMethodName = SERVICE_NAME + '/' + "CreateConnector",
            requestType = CreateConnectorRequest.class,
            responseType = ConfigReply.class,
            methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
    public static io.grpc.MethodDescriptor<CreateConnectorRequest,
            ConfigReply> getCreateConnectorMethod() {
        io.grpc.MethodDescriptor<CreateConnectorRequest, ConfigReply> getCreateConnectorMethod;
        if ((getCreateConnectorMethod = ConfigServiceGrpc.getCreateConnectorMethod) == null) {
            synchronized (ConfigServiceGrpc.class) {
                if ((getCreateConnectorMethod = ConfigServiceGrpc.getCreateConnectorMethod) == null) {
                    ConfigServiceGrpc.getCreateConnectorMethod = getCreateConnectorMethod =
                            io.grpc.MethodDescriptor.<CreateConnectorRequest, ConfigReply>newBuilder()
                                    .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                    .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateConnector"))
                                    .setSampledToLocalTracing(true)
                                    .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            CreateConnectorRequest.getDefaultInstance()))
                                    .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                                            ConfigReply.getDefaultInstance()))
                                    .setSchemaDescriptor(new ConfigServiceMethodDescriptorSupplier("CreateConnector"))
                                    .build();
                }
            }
        }
        return getCreateConnectorMethod;
    }

    /**
     * Creates a new async stub that supports all call types for the service
     */
    public static ConfigServiceStub newStub(io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<ConfigServiceStub> factory = new io.grpc.stub.AbstractStub.StubFactory<ConfigServiceStub>() {
            @Override
            public ConfigServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new ConfigServiceStub(channel, callOptions);
            }
        };
        return ConfigServiceStub.newStub(factory, channel);
    }

    /**
     * Creates a new blocking-style stub that supports unary and streaming output calls on the service
     */
    public static ConfigServiceBlockingStub newBlockingStub(
            io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<ConfigServiceBlockingStub> factory = new io.grpc.stub.AbstractStub.StubFactory<ConfigServiceBlockingStub>() {
            @Override
            public ConfigServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new ConfigServiceBlockingStub(channel, callOptions);
            }
        };
        return ConfigServiceBlockingStub.newStub(factory, channel);
    }

    /**
     * Creates a new ListenableFuture-style stub that supports unary calls on the service
     */
    public static ConfigServiceFutureStub newFutureStub(
            io.grpc.Channel channel) {
        io.grpc.stub.AbstractStub.StubFactory<ConfigServiceFutureStub> factory = new io.grpc.stub.AbstractStub.StubFactory<ConfigServiceFutureStub>() {
            @Override
            public ConfigServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
                return new ConfigServiceFutureStub(channel, callOptions);
            }
        };
        return ConfigServiceFutureStub.newStub(factory, channel);
    }

    /**
     *
     */
    public static abstract class ConfigServiceImplBase implements io.grpc.BindableService {

        /**
         *
         */
        public void stopConnector(StopConnectorRequest request,
                                  io.grpc.stub.StreamObserver<ConfigReply> responseObserver) {
            asyncUnimplementedUnaryCall(getStopConnectorMethod(), responseObserver);
        }

        /**
         *
         */
        public void stopAll(StopAllRequest request,
                            io.grpc.stub.StreamObserver<ConfigReply> responseObserver) {
            asyncUnimplementedUnaryCall(getStopAllMethod(), responseObserver);
        }

        /**
         *
         */
        public void createConnector(CreateConnectorRequest request,
                                    io.grpc.stub.StreamObserver<ConfigReply> responseObserver) {
            asyncUnimplementedUnaryCall(getCreateConnectorMethod(), responseObserver);
        }

        @Override
        public final io.grpc.ServerServiceDefinition bindService() {
            return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
                    .addMethod(getStopConnectorMethod(), asyncUnaryCall(
                            new MethodHandlers<StopConnectorRequest, ConfigReply>(this, METHODID_STOP_CONNECTOR)))
                    .addMethod(getStopAllMethod(), asyncUnaryCall(
                            new MethodHandlers<StopAllRequest, ConfigReply>(this, METHODID_STOP_ALL)))
                    .addMethod(getCreateConnectorMethod(), asyncUnaryCall(
                            new MethodHandlers<CreateConnectorRequest, ConfigReply>(this, METHODID_CREATE_CONNECTOR)))
                    .build();
        }
    }

    /**
     *
     */
    public static final class ConfigServiceStub extends io.grpc.stub.AbstractAsyncStub<ConfigServiceStub> {
        private ConfigServiceStub(
                io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @Override
        protected ConfigServiceStub build(
                io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new ConfigServiceStub(channel, callOptions);
        }

        /**
         *
         */
        public void stopConnector(StopConnectorRequest request,
                                  io.grpc.stub.StreamObserver<ConfigReply> responseObserver) {
            asyncUnaryCall(getChannel().newCall(getStopConnectorMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         *
         */
        public void stopAll(StopAllRequest request,
                            io.grpc.stub.StreamObserver<ConfigReply> responseObserver) {
            asyncUnaryCall(getChannel().newCall(getStopAllMethod(), getCallOptions()), request, responseObserver);
        }

        /**
         *
         */
        public void createConnector(CreateConnectorRequest request,
                                    io.grpc.stub.StreamObserver<ConfigReply> responseObserver) {
            asyncUnaryCall(getChannel().newCall(getCreateConnectorMethod(), getCallOptions()), request, responseObserver);
        }
    }

    /**
     *
     */
    public static final class ConfigServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<ConfigServiceBlockingStub> {
        private ConfigServiceBlockingStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @Override
        protected ConfigServiceBlockingStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new ConfigServiceBlockingStub(channel, callOptions);
        }

        /**
         *
         */
        public ConfigReply stopConnector(StopConnectorRequest request) {
            return blockingUnaryCall(
                    getChannel(), getStopConnectorMethod(), getCallOptions(), request);
        }

        /**
         *
         */
        public ConfigReply stopAll(StopAllRequest request) {
            return blockingUnaryCall(
                    getChannel(), getStopAllMethod(), getCallOptions(), request);
        }

        /**
         *
         */
        public ConfigReply createConnector(CreateConnectorRequest request) {
            return blockingUnaryCall(
                    getChannel(), getCreateConnectorMethod(), getCallOptions(), request);
        }
    }

    /**
     *
     */
    public static final class ConfigServiceFutureStub extends io.grpc.stub.AbstractFutureStub<ConfigServiceFutureStub> {
        private ConfigServiceFutureStub(
                io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @Override
        protected ConfigServiceFutureStub build(
                io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
            return new ConfigServiceFutureStub(channel, callOptions);
        }

        /**
         *
         */
        public com.google.common.util.concurrent.ListenableFuture<ConfigReply> stopConnector(
                StopConnectorRequest request) {
            return futureUnaryCall(
                    getChannel().newCall(getStopConnectorMethod(), getCallOptions()), request);
        }

        /**
         *
         */
        public com.google.common.util.concurrent.ListenableFuture<ConfigReply> stopAll(
                StopAllRequest request) {
            return futureUnaryCall(
                    getChannel().newCall(getStopAllMethod(), getCallOptions()), request);
        }

        /**
         *
         */
        public com.google.common.util.concurrent.ListenableFuture<ConfigReply> createConnector(
                CreateConnectorRequest request) {
            return futureUnaryCall(
                    getChannel().newCall(getCreateConnectorMethod(), getCallOptions()), request);
        }
    }

    private static final int METHODID_STOP_CONNECTOR = 0;
    private static final int METHODID_STOP_ALL = 1;
    private static final int METHODID_CREATE_CONNECTOR = 2;

    private static final class MethodHandlers<REQ, RESP> implements
            io.grpc.stub.ServerCalls.UnaryMethod<REQ, RESP>,
            io.grpc.stub.ServerCalls.ServerStreamingMethod<REQ, RESP>,
            io.grpc.stub.ServerCalls.ClientStreamingMethod<REQ, RESP>,
            io.grpc.stub.ServerCalls.BidiStreamingMethod<REQ, RESP> {
        private final ConfigServiceImplBase serviceImpl;
        private final int methodId;

        MethodHandlers(ConfigServiceImplBase serviceImpl, int methodId) {
            this.serviceImpl = serviceImpl;
            this.methodId = methodId;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void invoke(REQ request, io.grpc.stub.StreamObserver<RESP> responseObserver) {
            switch (methodId) {
                case METHODID_STOP_CONNECTOR:
                    serviceImpl.stopConnector((StopConnectorRequest) request,
                            (io.grpc.stub.StreamObserver<ConfigReply>) responseObserver);
                    break;
                case METHODID_STOP_ALL:
                    serviceImpl.stopAll((StopAllRequest) request,
                            (io.grpc.stub.StreamObserver<ConfigReply>) responseObserver);
                    break;
                case METHODID_CREATE_CONNECTOR:
                    serviceImpl.createConnector((CreateConnectorRequest) request,
                            (io.grpc.stub.StreamObserver<ConfigReply>) responseObserver);
                    break;
                default:
                    throw new AssertionError();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public io.grpc.stub.StreamObserver<REQ> invoke(
                io.grpc.stub.StreamObserver<RESP> responseObserver) {
            switch (methodId) {
                default:
                    throw new AssertionError();
            }
        }
    }

    private static abstract class ConfigServiceBaseDescriptorSupplier
            implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
        ConfigServiceBaseDescriptorSupplier() {
        }

        @Override
        public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
            return ConfigProto.getDescriptor();
        }

        @Override
        public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
            return getFileDescriptor().findServiceByName("ConfigService");
        }
    }

    private static final class ConfigServiceFileDescriptorSupplier
            extends ConfigServiceBaseDescriptorSupplier {
        ConfigServiceFileDescriptorSupplier() {
        }
    }

    private static final class ConfigServiceMethodDescriptorSupplier
            extends ConfigServiceBaseDescriptorSupplier
            implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
        private final String methodName;

        ConfigServiceMethodDescriptorSupplier(String methodName) {
            this.methodName = methodName;
        }

        @Override
        public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
            return getServiceDescriptor().findMethodByName(methodName);
        }
    }

    private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

    public static io.grpc.ServiceDescriptor getServiceDescriptor() {
        io.grpc.ServiceDescriptor result = serviceDescriptor;
        if (result == null) {
            synchronized (ConfigServiceGrpc.class) {
                result = serviceDescriptor;
                if (result == null) {
                    serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                            .setSchemaDescriptor(new ConfigServiceFileDescriptorSupplier())
                            .addMethod(getStopConnectorMethod())
                            .addMethod(getStopAllMethod())
                            .addMethod(getCreateConnectorMethod())
                            .build();
                }
            }
        }
        return result;
    }
}
