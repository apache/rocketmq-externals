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

package org.apache.connect.mongo.replicator;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.connect.mongo.SourceTaskConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoClientFactory {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private SourceTaskConfig taskConfig;

    public MongoClientFactory(SourceTaskConfig sourceTaskConfig) {
        this.taskConfig = sourceTaskConfig;
    }

    public MongoClient createMongoClient(ReplicaSetConfig replicaSetConfig) {
        StringBuilder sb = new StringBuilder();
        sb.append("mongodb://");
        if (StringUtils.isNotBlank(taskConfig.getMongoUserName())
            && StringUtils.isNotBlank(taskConfig.getMongoPassWord())) {
            sb.append(taskConfig.getMongoUserName());
            sb.append(":");
            sb.append(taskConfig.getMongoPassWord());
            sb.append("@");

        }
        sb.append(replicaSetConfig.getHost());
        sb.append("/");
        if (StringUtils.isNotBlank(replicaSetConfig.getReplicaSetName())) {
            sb.append("?");
            sb.append("replicaSet=");
            sb.append(replicaSetConfig.getReplicaSetName());
        }

        if (taskConfig.getServerSelectionTimeoutMS() > 0) {
            sb.append("&");
            sb.append("serverSelectionTimeoutMS=");
            sb.append(taskConfig.getServerSelectionTimeoutMS());
        }

        if (taskConfig.getConnectTimeoutMS() > 0) {
            sb.append("&");
            sb.append("connectTimeoutMS=");
            sb.append(taskConfig.getConnectTimeoutMS());
        }

        if (taskConfig.getSocketTimeoutMS() > 0) {
            sb.append("&");
            sb.append("socketTimeoutMS=");
            sb.append(taskConfig.getSocketTimeoutMS());
        }

        if (taskConfig.getSsl() || taskConfig.getTsl()) {
            sb.append("&");
            sb.append("ssl=");
            sb.append(true);
        }

        if (taskConfig.getTlsInsecure()) {
            sb.append("&");
            sb.append("tlsInsecure=");
            sb.append(true);
        }

        if (taskConfig.getTlsAllowInvalidHostnames()) {
            sb.append("&");
            sb.append("tlsAllowInvalidHostnames=");
            sb.append(true);
        }

        if (taskConfig.getSslInvalidHostNameAllowed()) {
            sb.append("&");
            sb.append("sslInvalidHostNameAllowed=");
            sb.append(true);
        }

        if (StringUtils.isNotBlank(taskConfig.getCompressors())) {
            sb.append("&");
            sb.append("compressors=");
            sb.append(taskConfig.getCompressors());
        }

        if (StringUtils.isNotBlank(taskConfig.getZlibCompressionLevel())) {
            sb.append("&");
            sb.append("zlibcompressionlevel=");
            sb.append(taskConfig.getZlibCompressionLevel());
        }

        if (StringUtils.isNotBlank(taskConfig.getTrustStore())) {
            Properties properties = System.getProperties();
            properties.put("javax.net.ssl.trustStore", taskConfig.getTrustStore());
            logger.info("javax.net.ssl.trustStore: {}", taskConfig.getTrustStore());
        }

        if (StringUtils.isNotBlank(taskConfig.getTrustStorePassword())) {
            Properties properties = System.getProperties();
            properties.put("javax.net.ssl.trustStorePassword", taskConfig.getTrustStorePassword());
            logger.info("javax.net.ssl.trustStorePassword: {}", taskConfig.getTrustStorePassword());
        }

        logger.info("connection string :{}", sb.toString());
        ConnectionString connectionString = new ConnectionString(sb.toString());
        return MongoClients.create(connectionString);
    }

}
