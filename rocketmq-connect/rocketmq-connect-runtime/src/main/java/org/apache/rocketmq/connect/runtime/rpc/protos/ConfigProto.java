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

public final class ConfigProto {
    private ConfigProto() {
    }

    public static void registerAllExtensions(
            com.google.protobuf.ExtensionRegistryLite registry) {
    }

    public static void registerAllExtensions(
            com.google.protobuf.ExtensionRegistry registry) {
        registerAllExtensions(
                (com.google.protobuf.ExtensionRegistryLite) registry);
    }

    static final com.google.protobuf.Descriptors.Descriptor
            INTERNAL_STATIC_STOP_CONNECTOR_REQUEST_DESCRIPTOR;
    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            INTERNAL_STATIC_STOP_CONNECTOR_REQIEST_FIELD_ACCESSOR_TABLE;
    static final com.google.protobuf.Descriptors.Descriptor
            INTERNAL_STATIC_STOP_ALL_REQUEST_DESCRIPTOR;
    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            INTERNAL_STATIC_STOP_ALL_REQUEST_FIELD_ACCESSOR_TABLE;
    static final com.google.protobuf.Descriptors.Descriptor
            INTERNAL_STATIC_CREATE_CONNECTOR_REQUEST_DESCRIPTOR;
    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            INTERNAL_STATIC_CREATE_CONNECTOR_REQUEST_FIELD_ACCESSOR_TABLE;
    static final com.google.protobuf.Descriptors.Descriptor
            INTERNAL_STATIC_CONFIG_REPLY_DESCRIPTOR;
    static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
            INTERNAL_STATIC_CONFIG_REPLY_FIELD_ACCESSOR_TABLE;

    public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
        return descriptor;
    }

    private static com.google.protobuf.Descriptors.FileDescriptor descriptor;

    static {
        String[] descriptorData = {
            "\n\014config.proto\022\026connectorAndTaskConfig\"-" +
                    "\n\024StopConnectorRequest\022\025\n\rconnectorName\030" +
                    "\001 \001(\t\"!\n\016StopAllRequest\022\017\n\007request\030\001 \001(\010" +
                    "\"?\n\026CreateConnectorRequest\022\025\n\rconnectorN" +
                    "ame\030\001 \001(\t\022\016\n\006config\030\002 \001(\t\"\035\n\013ConfigReply" +
                    "\022\016\n\006result\030\001 \001(\t2\271\002\n\rConfigService\022d\n\rSt" +
                    "opConnector\022,.connectorAndTaskConfig.Sto" +
                    "pConnectorRequest\032#.connectorAndTaskConf" +
                    "ig.ConfigReply\"\000\022X\n\007StopAll\022&.connectorA" +
                    "ndTaskConfig.StopAllRequest\032#.connectorA" +
                    "ndTaskConfig.ConfigReply\"\000\022h\n\017CreateConn" +
                    "ector\022..connectorAndTaskConfig.CreateCon" +
                    "nectorRequest\032#.connectorAndTaskConfig.C" +
                    "onfigReply\"\000B%\n\016io.grpc.configB\013ConfigPr" +
                    "otoP\001\242\002\003CATb\006proto3"
        };
        descriptor = com.google.protobuf.Descriptors.FileDescriptor
                .internalBuildGeneratedFileFrom(descriptorData,
                        new com.google.protobuf.Descriptors.FileDescriptor[]{
                        });
        INTERNAL_STATIC_STOP_CONNECTOR_REQUEST_DESCRIPTOR =
                getDescriptor().getMessageTypes().get(0);
        INTERNAL_STATIC_STOP_CONNECTOR_REQIEST_FIELD_ACCESSOR_TABLE = new
                com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                INTERNAL_STATIC_STOP_CONNECTOR_REQUEST_DESCRIPTOR,
                new String[]{"ConnectorName", });
        INTERNAL_STATIC_STOP_ALL_REQUEST_DESCRIPTOR =
                getDescriptor().getMessageTypes().get(1);
        INTERNAL_STATIC_STOP_ALL_REQUEST_FIELD_ACCESSOR_TABLE = new
                com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                INTERNAL_STATIC_STOP_ALL_REQUEST_DESCRIPTOR,
                new String[]{"Request", });
        INTERNAL_STATIC_CREATE_CONNECTOR_REQUEST_DESCRIPTOR =
                getDescriptor().getMessageTypes().get(2);
        INTERNAL_STATIC_CREATE_CONNECTOR_REQUEST_FIELD_ACCESSOR_TABLE = new
                com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                INTERNAL_STATIC_CREATE_CONNECTOR_REQUEST_DESCRIPTOR,
                new String[]{"ConnectorName", "Config", });
        INTERNAL_STATIC_CONFIG_REPLY_DESCRIPTOR =
                getDescriptor().getMessageTypes().get(3);
        INTERNAL_STATIC_CONFIG_REPLY_FIELD_ACCESSOR_TABLE = new
                com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                INTERNAL_STATIC_CONFIG_REPLY_DESCRIPTOR,
                new String[]{"Result", });
    }

    // @@protoc_insertion_point(outer_class_scope)
}
