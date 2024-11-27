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
package org.apache.rocketmq.tieredstore.s3.object;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public class S3URI {
    private static final String ENDPOINT_KEY = "endpoint";
    private static final String REGION_KEY = "region";
    public static final String ACCESS_KEY_KEY = "accessKey";
    public static final String SECRET_KEY_KEY = "secretKey";
    private static final String EMPTY_STRING = "";
    private final String bucket;
    private final String region;
    private String endpoint;
    private final Map<String, List<String>> extension;

    private S3URI(String bucket, String region, String endpoint,
        Map<String, List<String>> extension) {
        this.bucket = bucket;
        this.region = region;
        this.endpoint = endpoint;
        this.extension = extension;
    }

    public String endpoint() {
        return endpoint;
    }

    public void endpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String bucket() {
        return bucket;
    }

    public String region() {
        return region;
    }

    public String extensionString(String key) {
        return getString(extension, key, null);
    }

    public String extensionString(String key, String defaultVal) {
        return getString(extension, key, defaultVal);
    }

    public boolean extensionBool(String key, boolean defaultVal) {
        String value = getString(extension, key, null);
        if (StringUtils.isBlank(value)) {
            return defaultVal;
        }
        return Boolean.parseBoolean(value);
    }

    public void addExtension(String key, String value) {
        extension.put(key, Lists.newArrayList(value));
    }

    private String getString(Map<String, List<String>> queries, String key, String defaultValue) {
        List<String> value = queries.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value.size() > 1) {
            throw new IllegalArgumentException("expect only one value for key: " + key + " but found " + value);
        }
        return value.get(0);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BucketURL{" +
            "bucket='" + bucket + '\'' +
            ", region='" + region + '\'' +
            ", endpoint='" + endpoint + '\'');
        sb.append(", extension={");
        extension.forEach((k, v) -> {
            sb.append(k).append("=");
            if (k.equals(SECRET_KEY_KEY)) {
                sb.append("*******");
            }
            else {
                sb.append(v);
            }
            sb.append(", ");
        });
        sb.append("}");
        return sb.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String bucket;
        private String region;
        private String endpoint;
        private Map<String, List<String>> extension;

        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder extension(Map<String, List<String>> extension) {
            this.extension = extension;
            return this;
        }

        public S3URI build() {
            return new S3URI(bucket, region, endpoint, extension);
        }

    }
}
