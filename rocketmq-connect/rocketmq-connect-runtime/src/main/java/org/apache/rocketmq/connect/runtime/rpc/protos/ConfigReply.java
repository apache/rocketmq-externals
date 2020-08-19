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
 * Protobuf type {@code connectorAndTaskConfig.ConfigReply}
 */
public final class ConfigReply extends
        com.google.protobuf.GeneratedMessageV3 implements
        // @@protoc_insertion_point(message_implements:connectorAndTaskConfig.ConfigReply)
        ConfigReplyOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use ConfigReply.newBuilder() to construct.
    private ConfigReply(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private ConfigReply() {
        result = "";
    }

    @Override
    @SuppressWarnings({"unused"})
    protected Object newInstance(
            UnusedPrivateParameter unused) {
        return new ConfigReply();
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    private ConfigReply(
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

                        result = s;
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
        return ConfigProto.INTERNAL_STATIC_CONFIG_REPLY_DESCRIPTOR;
    }

    @Override
    protected FieldAccessorTable internalGetFieldAccessorTable() {
        return ConfigProto.INTERNAL_STATIC_CONFIG_REPLY_FIELD_ACCESSOR_TABLE
                .ensureFieldAccessorsInitialized(
                        ConfigReply.class, ConfigReply.Builder.class);
    }

    public static final int RESULT_FIELD_NUMBER = 1;
    private volatile Object result;

    /**
     * <code>string result = 1;</code>
     *
     * @return The result.
     */
    @Override
    public String getResult() {
        Object ref = result;
        if (ref instanceof String) {
            return (String) ref;
        } else {
            com.google.protobuf.ByteString bs =
                    (com.google.protobuf.ByteString) ref;
            String s = bs.toStringUtf8();
            result = s;
            return s;
        }
    }

    /**
     * <code>string result = 1;</code>
     *
     * @return The bytes for result.
     */
    @Override
    public com.google.protobuf.ByteString getResultBytes() {
        Object ref = result;
        if (ref instanceof String) {
            com.google.protobuf.ByteString b =
                    com.google.protobuf.ByteString.copyFromUtf8(
                            (String) ref);
            result = b;
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
        if (!getResultBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 1, result);
        }
        unknownFields.writeTo(output);
    }

    @Override
    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1) return size;

        size = 0;
        if (!getResultBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, result);
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
        if (!(obj instanceof ConfigReply)) {
            return super.equals(obj);
        }
        ConfigReply other = (ConfigReply) obj;

        if (!getResult()
                .equals(other.getResult())) return false;
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
        hash = (37 * hash) + RESULT_FIELD_NUMBER;
        hash = (53 * hash) + getResult().hashCode();
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public static ConfigReply parseFrom(
            java.nio.ByteBuffer data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static ConfigReply parseFrom(
            java.nio.ByteBuffer data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ConfigReply parseFrom(
            com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static ConfigReply parseFrom(
            com.google.protobuf.ByteString data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ConfigReply parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static ConfigReply parseFrom(
            byte[] data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static ConfigReply parseFrom(java.io.InputStream input)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input);
    }

    public static ConfigReply parseFrom(
            java.io.InputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static ConfigReply parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseDelimitedWithIOException(PARSER, input);
    }

    public static ConfigReply parseDelimitedFrom(
            java.io.InputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static ConfigReply parseFrom(
            com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input);
    }

    public static ConfigReply parseFrom(
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

    public static Builder newBuilder(ConfigReply prototype) {
        return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    @Override
    public Builder toBuilder() {
        return this == DEFAULT_INSTANCE
                ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(BuilderParent parent) {
        Builder builder = new Builder(parent);
        return builder;
    }

    /**
     * Protobuf type {@code connectorAndTaskConfig.ConfigReply}
     */
    public static final class Builder extends
            com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
            // @@protoc_insertion_point(builder_implements:connectorAndTaskConfig.ConfigReply)
            ConfigReplyOrBuilder {
        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return ConfigProto.INTERNAL_STATIC_CONFIG_REPLY_DESCRIPTOR;
        }

        @Override
        protected FieldAccessorTable internalGetFieldAccessorTable() {
            return ConfigProto.INTERNAL_STATIC_CONFIG_REPLY_FIELD_ACCESSOR_TABLE
                    .ensureFieldAccessorsInitialized(
                            ConfigReply.class, ConfigReply.Builder.class);
        }

        // Construct using io.grpc.config.ConfigReply.newBuilder()
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
            result = "";

            return this;
        }

        @Override
        public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
            return ConfigProto.INTERNAL_STATIC_CONFIG_REPLY_DESCRIPTOR;
        }

        @Override
        public ConfigReply getDefaultInstanceForType() {
            return ConfigReply.getDefaultInstance();
        }

        @Override
        public ConfigReply build() {
            ConfigReply result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        @Override
        public ConfigReply buildPartial() {
            ConfigReply result = new ConfigReply(this);
            result.result = this.result;
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
            if (other instanceof ConfigReply) {
                return mergeFrom((ConfigReply) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(ConfigReply other) {
            if (other == ConfigReply.getDefaultInstance()) return this;
            if (!other.getResult().isEmpty()) {
                result = other.result;
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
            ConfigReply parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (ConfigReply) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private Object result = "";

        /**
         * <code>string result = 1;</code>
         *
         * @return The result.
         */
        public String getResult() {
            Object ref = result;
            if (!(ref instanceof String)) {
                com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
                String s = bs.toStringUtf8();
                result = s;
                return s;
            } else {
                return (String) ref;
            }
        }

        /**
         * <code>string result = 1;</code>
         *
         * @return The bytes for result.
         */
        public com.google.protobuf.ByteString getResultBytes() {
            Object ref = result;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
                result = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <code>string result = 1;</code>
         *
         * @param value The result to set.
         * @return This builder for chaining.
         */
        public Builder setResult(String value) {
            if (value == null) {
                throw new NullPointerException();
            }

            result = value;
            onChanged();
            return this;
        }

        /**
         * <code>string result = 1;</code>
         *
         * @return This builder for chaining.
         */
        public Builder clearResult() {

            result = getDefaultInstance().getResult();
            onChanged();
            return this;
        }

        /**
         * <code>string result = 1;</code>
         *
         * @param value The bytes for result to set.
         * @return This builder for chaining.
         */
        public Builder setResultBytes(com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);

            result = value;
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


        // @@protoc_insertion_point(builder_scope:connectorAndTaskConfig.ConfigReply)
    }

    // @@protoc_insertion_point(class_scope:connectorAndTaskConfig.ConfigReply)
    private static final ConfigReply DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = new ConfigReply();
    }

    public static ConfigReply getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<ConfigReply> PARSER = new com.google.protobuf.AbstractParser<ConfigReply>() {
        @Override
        public ConfigReply parsePartialFrom(
                com.google.protobuf.CodedInputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return new ConfigReply(input, extensionRegistry);
        }
    };

    public static com.google.protobuf.Parser<ConfigReply> parser() {
        return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<ConfigReply> getParserForType() {
        return PARSER;
    }

    @Override
    public ConfigReply getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }

}

