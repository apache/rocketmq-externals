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

        if (StringUtils.isNotBlank(taskConfig.getServerSelectionTimeoutMS())) {
            sb.append("&");
            sb.append("serverSelectionTimeoutMS=");
            sb.append(taskConfig.getServerSelectionTimeoutMS());
        }

        if (StringUtils.isNotBlank(taskConfig.getConnectTimeoutMS())) {
            sb.append("&");
            sb.append("connectTimeoutMS=");
            sb.append(taskConfig.getConnectTimeoutMS());
        }

        if (StringUtils.isNotBlank(taskConfig.getSocketTimeoutMS())) {
            sb.append("&");
            sb.append("socketTimeoutMS=");
            sb.append(taskConfig.getSocketTimeoutMS());
        }

        if (StringUtils.isNotBlank(taskConfig.getSsl()) || StringUtils.isNotBlank(taskConfig.getTsl())) {
            sb.append("&");
            sb.append("ssl=");
            sb.append(true);
        }

        if (StringUtils.isNotBlank(taskConfig.getTlsInsecure())) {
            sb.append("&");
            sb.append("tlsInsecure=");
            sb.append(taskConfig.getTlsInsecure());
        }

        if (StringUtils.isNotBlank(taskConfig.getTlsAllowInvalidHostnames())) {
            sb.append("&");
            sb.append("tlsAllowInvalidHostnames=");
            sb.append(taskConfig.getTlsAllowInvalidHostnames());
        }

        if (StringUtils.isNotBlank(taskConfig.getSslInvalidHostNameAllowed())) {
            sb.append("&");
            sb.append("sslInvalidHostNameAllowed=");
            sb.append(taskConfig.getSslInvalidHostNameAllowed());
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
        System.out.println(sb.toString());
        ConnectionString connectionString = new ConnectionString(sb.toString());
        return MongoClients.create(connectionString);
    }

}
