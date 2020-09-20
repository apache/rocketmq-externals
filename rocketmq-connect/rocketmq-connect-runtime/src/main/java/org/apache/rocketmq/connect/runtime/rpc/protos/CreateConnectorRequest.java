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

/**
 * Protobuf type {@code connectorAndTaskConfig.CreateConnectorRequest}
 */
public final class CreateConnectorRequest extends
        com.google.protobuf.GeneratedMessageV3 implements
        // @@protoc_insertion_point(message_implements:connectorAndTaskConfig.CreateConnectorRequest)
        CreateConnectorRequestOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use CreateConnectorRequest.newBuilder() to construct.
    private CreateConnectorRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private CreateConnectorRequest() {
        connectorName = "";
        config = "";
    }

    @Override
    @SuppressWarnings({"unused"})
    protected Object newInstance(
            UnusedPrivateParameter unused) {
        return new CreateConnectorRequest();
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    private CreateConnectorRequest(
            com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        this();
        if (extensionRegistry == null) {
            throw new NullPointerException();
        }
        com.google.protobuf.UnknownFieldSet.Builder unknownFields =
                com.google.protobuf.UnknownFieldSet.newBuilder();
        try {
            boolean done = false;
            while (!done) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        done = true;
                        break;
                    case 10: {
                        String s = input.readStringRequireUtf8();

                        connectorName = s;
                        break;
                    }
                    case 18: {
                        String s = input.readStringRequireUtf8();

                        config = s;
                        break;
                    }
                    default: {
                        if (!parseUnknownField(
                                input, unknownFields, extensionRegistry, tag)) {
                            done = true;
                        }
                        break;
                    }
                }
            }
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
            throw e.setUnfinishedMessage(this);
        } catch (java.io.IOException e) {
            throw new com.google.protobuf.InvalidProtocolBufferException(
                    e).setUnfinishedMessage(this);
        } finally {
            this.unknownFields = unknownFields.build();
            makeExtensionsImmutable();
        }
    }

    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
        return ConfigProto.INTERNAL_STATIC_CREATE_CONNECTOR_REQUEST_DESCRIPTOR;
    }

    @Override
    protected FieldAccessorTable internalGetFieldAccessorTable() {
        return ConfigProto.INTERNAL_STATIC_CREATE_CONNECTOR_REQUEST_FIELD_ACCESSOR_TABLE
                .ensureFieldAccessorsInitialized(
                        CreateConnectorRequest.class, CreateConnectorRequest.Builder.class);
    }

    public static final int CONNECTORNAME_FIELD_NUMBER = 1;
    private volatile Object connectorName;

    /**
     * <code>string connectorName = 1;</code>
     *
     * @return The connectorName.
     */
    @Override
    public String getConnectorName() {
        Object ref = connectorName;
        if (ref instanceof String) {
            return (String) ref;
        } else {
            com.google.protobuf.ByteString bs =
                    (com.google.protobuf.ByteString) ref;
            String s = bs.toStringUtf8();
            connectorName = s;
            return s;
        }
    }

    /**
     * <code>string connectorName = 1;</code>
     *
     * @return The bytes for connectorName.
     */
    @Override
    public com.google.protobuf.ByteString getConnectorNameBytes() {
        Object ref = connectorName;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b =
                    com.google.protobuf.ByteString.copyFromUtf8(
                            (String) ref);
            connectorName = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    public static final int CONFIG_FIELD_NUMBER = 2;
    private volatile Object config;

    /**
     * <code>string config = 2;</code>
     *
     * @return The config.
     */
    @Override
    public String getConfig() {
        Object ref = config;
        if (ref instanceof String) {
            return (String) ref;
        } else {
            com.google.protobuf.ByteString bs =
                    (com.google.protobuf.ByteString) ref;
            String s = bs.toStringUtf8();
            config = s;
            return s;
        }
    }

    /**
     * <code>string config = 2;</code>
     *
     * @return The bytes for config.
     */
    @Override
    public com.google.protobuf.ByteString getConfigBytes() {
        Object ref = config;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b =
                    com.google.protobuf.ByteString.copyFromUtf8(
                            (String) ref);
            config = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    private byte memoizedIsInitialized = -1;

    @Override
    public final boolean isInitialized() {
        byte isInitialized = memoizedIsInitialized;
        if (isInitialized == 1) return true;
        if (isInitialized == 0) return false;

        memoizedIsInitialized = 1;
        return true;
    }

    @Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
            throws java.io.IOException {
        if (!getConnectorNameBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 1, connectorName);
        }
        if (!getConfigBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 2, config);
        }
        unknownFields.writeTo(output);
    }

    @Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1) return size;

        size = 0;
        if (!getConnectorNameBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, connectorName);
        }
        if (!getConfigBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, config);
        }
        size += unknownFields.getSerializedSize();
        memoizedSize = size;
        return size;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CreateConnectorRequest)) {
            return super.equals(obj);
        }
        CreateConnectorRequest other = (CreateConnectorRequest) obj;

        if (!getConnectorName()
                .equals(other.getConnectorName())) return false;
        if (!getConfig()
                .equals(other.getConfig())) return false;
        if (!unknownFields.equals(other.unknownFields)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        if (memoizedHashCode != 0) {
            return memoizedHashCode;
        }
        int hash = 41;
        hash = (19 * hash) + getDescriptor().hashCode();
        hash = (37 * hash) + CONNECTORNAME_FIELD_NUMBER;
        hash = (53 * hash) + getConnectorName().hashCode();
        hash = (37 * hash) + CONFIG_FIELD_NUMBER;
        hash = (53 * hash) + getConfig().hashCode();
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static CreateConnectorRequest parseFrom(
            java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static CreateConnectorRequest parseFrom(
            java.nio.ByteBuffer data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static CreateConnectorRequest parseFrom(
            com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static CreateConnectorRequest parseFrom(
            com.google.protobuf.ByteString data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static CreateConnectorRequest parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static CreateConnectorRequest parseFrom(
            byte[] data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static CreateConnectorRequest parseFrom(java.io.InputStream input)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input);
    }

    public static CreateConnectorRequest parseFrom(
            java.io.InputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static CreateConnectorRequest parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseDelimitedWithIOException(PARSER, input);
    }

    public static CreateConnectorRequest parseDelimitedFrom(
            java.io.InputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static CreateConnectorRequest parseFrom(
            com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input);
    }

    public static CreateConnectorRequest parseFrom(
            com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input, extensionRegistry);
    }

    @Override
    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(CreateConnectorRequest prototype) {
        return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    @Override
    public Builder toBuilder() {
        return this == DEFAULT_INSTANCE
                ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(
            BuilderParent parent) {
        Builder builder = new Builder(parent);
        return builder;
    }

    /**
     * Protobuf type {@code connectorAndTaskConfig.CreateConnectorRequest}
     */
    public static final class Builder extends
            com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
            // @@protoc_insertion_point(builder_implements:connectorAndTaskConfig.CreateConnectorRequest)
            CreateConnectorRequestOrBuilder {
        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return ConfigProto.INTERNAL_STATIC_CREATE_CONNECTOR_REQUEST_DESCRIPTOR;
        }

        @Override
        protected FieldAccessorTable internalGetFieldAccessorTable() {
            return ConfigProto.INTERNAL_STATIC_CREATE_CONNECTOR_REQUEST_FIELD_ACCESSOR_TABLE
                    .ensureFieldAccessorsInitialized(
                            CreateConnectorRequest.class, CreateConnectorRequest.Builder.class);
        }

        // Construct using io.grpc.config.CreateConnectorRequest.newBuilder()
        private Builder() {
            maybeForceBuilderInitialization();
        }

        private Builder(BuilderParent parent) {
            super(parent);
            maybeForceBuilderInitialization();
        }

        private void maybeForceBuilderInitialization() {
            if (com.google.protobuf.GeneratedMessageV3
                    .alwaysUseFieldBuilders) {
            }
        }

        @Override
        public Builder clear() {
            super.clear();
            connectorName = "";

            config = "";

            return this;
        }

        @Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return ConfigProto.INTERNAL_STATIC_CREATE_CONNECTOR_REQUEST_DESCRIPTOR;
        }

        @Override
        public CreateConnectorRequest getDefaultInstanceForType() {
            return CreateConnectorRequest.getDefaultInstance();
        }

        @Override
        public CreateConnectorRequest build() {
            CreateConnectorRequest result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @Override
        public CreateConnectorRequest buildPartial() {
            CreateConnectorRequest result = new CreateConnectorRequest(this);
            result.connectorName = connectorName;
            result.config = config;
            onBuilt();
            return result;
        }

        @Override
        public Builder clone() {
            return super.clone();
        }

        @Override
        public Builder setField(
                com.google.protobuf.Descriptors.FieldDescriptor field,
                Object value) {
            return super.setField(field, value);
        }

        @Override
        public Builder clearField(
                com.google.protobuf.Descriptors.FieldDescriptor field) {
            return super.clearField(field);
        }

        @Override
        public Builder clearOneof(
                com.google.protobuf.Descriptors.OneofDescriptor oneof) {
            return super.clearOneof(oneof);
        }

        @Override
        public Builder setRepeatedField(
                com.google.protobuf.Descriptors.FieldDescriptor field,
                int index, Object value) {
            return super.setRepeatedField(field, index, value);
        }

        @Override
        public Builder addRepeatedField(
                com.google.protobuf.Descriptors.FieldDescriptor field,
                Object value) {
            return super.addRepeatedField(field, value);
        }

        @Override
        public Builder mergeFrom(com.google.protobuf.Message other) {
            if (other instanceof CreateConnectorRequest) {
                return mergeFrom((CreateConnectorRequest) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(CreateConnectorRequest other) {
            if (other == CreateConnectorRequest.getDefaultInstance()) return this;
            if (!other.getConnectorName().isEmpty()) {
                connectorName = other.connectorName;
                onChanged();
            }
            if (!other.getConfig().isEmpty()) {
                config = other.config;
                onChanged();
            }
            this.mergeUnknownFields(other.unknownFields);
            onChanged();
            return this;
        }

        @Override
        public final boolean isInitialized() {
            return true;
        }

        @Override
        public Builder mergeFrom(
                com.google.protobuf.CodedInputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            CreateConnectorRequest parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (CreateConnectorRequest) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private Object connectorName = "";

        /**
         * <code>string connectorName = 1;</code>
         *
         * @return The connectorName.
         */
        public String getConnectorName() {
            Object ref = connectorName;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs =
                        (com.google.protobuf.ByteString) ref;
                String s = bs.toStringUtf8();
                connectorName = s;
                return s;
            } else {
                return (String) ref;
            }
        }

        /**
         * <code>string connectorName = 1;</code>
         *
         * @return The bytes for connectorName.
         */
        public com.google.protobuf.ByteString getConnectorNameBytes() {
            Object ref = connectorName;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b =
                        com.google.protobuf.ByteString.copyFromUtf8(
                                (String) ref);
                connectorName = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <code>string connectorName = 1;</code>
         *
         * @param value The connectorName to set.
         * @return This builder for chaining.
         */
        public Builder setConnectorName(
                String value) {
            if (value == null) {
                throw new NullPointerException();
            }

            connectorName = value;
            onChanged();
            return this;
        }

        /**
         * <code>string connectorName = 1;</code>
         *
         * @return This builder for chaining.
         */
        public Builder clearConnectorName() {

            connectorName = getDefaultInstance().getConnectorName();
            onChanged();
            return this;
        }

        /**
         * <code>string connectorName = 1;</code>
         *
         * @param value The bytes for connectorName to set.
         * @return This builder for chaining.
         */
        public Builder setConnectorNameBytes(
                com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);

            connectorName = value;
            onChanged();
            return this;
        }

        private Object config = "";

        /**
         * <code>string config = 2;</code>
         *
         * @return The config.
         */
        public String getConfig() {
            Object ref = config;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs =
                        (com.google.protobuf.ByteString) ref;
                String s = bs.toStringUtf8();
                config = s;
                return s;
            } else {
                return (String) ref;
            }
        }

        /**
         * <code>string config = 2;</code>
         *
         * @return The bytes for config.
         */
        public com.google.protobuf.ByteString getConfigBytes() {
            Object ref = config;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b =
                        com.google.protobuf.ByteString.copyFromUtf8(
                                (String) ref);
                config = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <code>string config = 2;</code>
         *
         * @param value The config to set.
         * @return This builder for chaining.
         */
        public Builder setConfig(
                String value) {
            if (value == null) {
                throw new NullPointerException();
            }

            config = value;
            onChanged();
            return this;
        }

        /**
         * <code>string config = 2;</code>
         *
         * @return This builder for chaining.
         */
        public Builder clearConfig() {

            config = getDefaultInstance().getConfig();
            onChanged();
            return this;
        }

        /**
         * <code>string config = 2;</code>
         *
         * @param value The bytes for config to set.
         * @return This builder for chaining.
         */
        public Builder setConfigBytes(
                com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);

            config = value;
            onChanged();
            return this;
        }

        @Override
        public final Builder setUnknownFields(
                final com.google.protobuf.UnknownFieldSet unknownFields) {
            return super.setUnknownFields(unknownFields);
        }

        @Override
        public final Builder mergeUnknownFields(
                final com.google.protobuf.UnknownFieldSet unknownFields) {
            return super.mergeUnknownFields(unknownFields);
        }


        // @@protoc_insertion_point(builder_scope:connectorAndTaskConfig.CreateConnectorRequest)
    }

    // @@protoc_insertion_point(class_scope:connectorAndTaskConfig.CreateConnectorRequest)
    private static final CreateConnectorRequest DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new CreateConnectorRequest();
    }

    public static CreateConnectorRequest getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<CreateConnectorRequest> PARSER = new com.google.protobuf.AbstractParser<CreateConnectorRequest>() {
        @Override
        public CreateConnectorRequest parsePartialFrom(com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws com.google.protobuf.InvalidProtocolBufferException {
            return new CreateConnectorRequest(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<CreateConnectorRequest> parser() {
        return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<CreateConnectorRequest> getParserForType() {
        return PARSER;
    }

    @Override
    public CreateConnectorRequest getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }

}

