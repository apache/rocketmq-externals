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
 * Protobuf type {@code connectorAndTaskConfig.StopAllRequest}
 */
public final class StopAllRequest extends
        com.google.protobuf.GeneratedMessageV3 implements
        // @@protoc_insertion_point(message_implements:connectorAndTaskConfig.StopAllRequest)
        StopAllRequestOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use StopAllRequest.newBuilder() to construct.
    private StopAllRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private StopAllRequest() {
    }

    @Override
    @SuppressWarnings({"unused"})
    protected Object newInstance(
            UnusedPrivateParameter unused) {
        return new StopAllRequest();
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    private StopAllRequest(
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
                    case 8: {

                        request = input.readBool();
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
        return ConfigProto.INTERNAL_STATIC_STOP_ALL_REQUEST_DESCRIPTOR;
    }

    @Override
    protected FieldAccessorTable internalGetFieldAccessorTable() {
        return ConfigProto.INTERNAL_STATIC_STOP_ALL_REQUEST_FIELD_ACCESSOR_TABLE
                .ensureFieldAccessorsInitialized(
                        StopAllRequest.class, StopAllRequest.Builder.class);
    }

    public static final int REQUEST_FIELD_NUMBER = 1;
    private boolean request;

    /**
     * <code>bool request = 1;</code>
     *
     * @return The request.
     */
    @Override
    public boolean getRequest() {
        return request;
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
        if (request) {
            output.writeBool(1, request);
        }
        unknownFields.writeTo(output);
    }

    @Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1) return size;

        size = 0;
        if (request) {
            size += com.google.protobuf.CodedOutputStream.computeBoolSize(1, request);
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
        if (!(obj instanceof StopAllRequest)) {
            return super.equals(obj);
        }
        StopAllRequest other = (StopAllRequest) obj;

        if (getRequest() != other.getRequest()) return false;
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
        hash = (37 * hash) + REQUEST_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(
                getRequest());
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static StopAllRequest parseFrom(
            java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static StopAllRequest parseFrom(
            java.nio.ByteBuffer data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static StopAllRequest parseFrom(
            com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static StopAllRequest parseFrom(
            com.google.protobuf.ByteString data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static StopAllRequest parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static StopAllRequest parseFrom(
            byte[] data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static StopAllRequest parseFrom(java.io.InputStream input)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input);
    }

    public static StopAllRequest parseFrom(
            java.io.InputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static StopAllRequest parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseDelimitedWithIOException(PARSER, input);
    }

    public static StopAllRequest parseDelimitedFrom(
            java.io.InputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static StopAllRequest parseFrom(
            com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input);
    }

    public static StopAllRequest parseFrom(
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

    public static Builder newBuilder(StopAllRequest prototype) {
        return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    @Override
    public Builder toBuilder() {
        return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(
            BuilderParent parent) {
        Builder builder = new Builder(parent);
        return builder;
    }

    /**
     * Protobuf type {@code connectorAndTaskConfig.StopAllRequest}
     */
    public static final class Builder extends
            com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
            // @@protoc_insertion_point(builder_implements:connectorAndTaskConfig.StopAllRequest)
            StopAllRequestOrBuilder {
        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return ConfigProto.INTERNAL_STATIC_STOP_ALL_REQUEST_DESCRIPTOR;
        }

        @Override
        protected FieldAccessorTable internalGetFieldAccessorTable() {
            return ConfigProto.INTERNAL_STATIC_STOP_ALL_REQUEST_FIELD_ACCESSOR_TABLE
                    .ensureFieldAccessorsInitialized(
                            StopAllRequest.class, StopAllRequest.Builder.class);
        }

        // Construct using io.grpc.config.StopAllRequest.newBuilder()
        private Builder() {
            maybeForceBuilderInitialization();
        }

        private Builder(
                BuilderParent parent) {
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
            request = false;

            return this;
        }

        @Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return ConfigProto.INTERNAL_STATIC_STOP_ALL_REQUEST_DESCRIPTOR;
        }

        @Override
        public StopAllRequest getDefaultInstanceForType() {
            return StopAllRequest.getDefaultInstance();
        }

        @Override
        public StopAllRequest build() {
            StopAllRequest result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @Override
        public StopAllRequest buildPartial() {
            StopAllRequest result = new StopAllRequest(this);
            result.request = request;
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
            if (other instanceof StopAllRequest) {
                return mergeFrom((StopAllRequest) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(StopAllRequest other) {
            if (other == StopAllRequest.getDefaultInstance()) return this;
            if (other.getRequest()) {
                setRequest(other.getRequest());
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
            StopAllRequest parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (StopAllRequest) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private boolean request;

        /**
         * <code>bool request = 1;</code>
         *
         * @return The request.
         */
        @Override
        public boolean getRequest() {
            return request;
        }

        /**
         * <code>bool request = 1;</code>
         *
         * @param value The request to set.
         * @return This builder for chaining.
         */
        public Builder setRequest(boolean value) {

            request = value;
            onChanged();
            return this;
        }

        /**
         * <code>bool request = 1;</code>
         *
         * @return This builder for chaining.
         */
        public Builder clearRequest() {

            request = false;
            onChanged();
            return this;
        }

        @Override
        public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
            return super.setUnknownFields(unknownFields);
        }

        @Override
        public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
            return super.mergeUnknownFields(unknownFields);
        }


        // @@protoc_insertion_point(builder_scope:connectorAndTaskConfig.StopAllRequest)
    }

    // @@protoc_insertion_point(class_scope:connectorAndTaskConfig.StopAllRequest)
    private static final StopAllRequest DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new StopAllRequest();
    }

    public static StopAllRequest getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<StopAllRequest> PARSER = new com.google.protobuf.AbstractParser<StopAllRequest>() {
        @Override
        public StopAllRequest parsePartialFrom(
                com.google.protobuf.CodedInputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return new StopAllRequest(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<StopAllRequest> parser() {
        return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<StopAllRequest> getParserForType() {
        return PARSER;
    }

    @Override
    public StopAllRequest getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }

}

