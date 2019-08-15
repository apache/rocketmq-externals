package org.apache.connect.mongo;

import io.openmessaging.KeyValue;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.bson.BsonTimestamp;

public class SourceTaskConfig {

    private String replicaSet;
    private String mongoAddr;
    private String mongoUserName;
    private String mongoPassWord;
    private String interestDbAndCollection;
    private String positionTimeStamp;
    private String positionInc;
    private String dataSync;
    private String serverSelectionTimeoutMS;
    private String connectTimeoutMS;
    private String socketTimeoutMS;
    private String ssl;
    private String tsl;
    private String tlsInsecure;
    private String sslInvalidHostNameAllowed;
    private String tlsAllowInvalidHostnames;
    private String compressors;
    private String zlibCompressionLevel;
    private String trustStore;
    private String trustStorePassword;
    private int copyThread = Runtime.getRuntime().availableProcessors();

    public static final Set<String> REQUEST_CONFIG = Collections.unmodifiableSet(new HashSet<String>() {
        {
            add("mongoAddr");
        }
    });

    public String getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(String trustStore) {
        this.trustStore = trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public String getZlibCompressionLevel() {
        return zlibCompressionLevel;
    }

    public void setZlibCompressionLevel(String zlibCompressionLevel) {
        this.zlibCompressionLevel = zlibCompressionLevel;
    }

    public String getPositionInc() {
        return positionInc;
    }

    public void setPositionInc(String positionInc) {
        this.positionInc = positionInc;
    }

    public int getCopyThread() {
        return copyThread;
    }

    public void setCopyThread(int copyThread) {
        this.copyThread = copyThread;
    }

    public String getPositionTimeStamp() {
        return positionTimeStamp;
    }

    public void setPositionTimeStamp(String positionTimeStamp) {
        this.positionTimeStamp = positionTimeStamp;
    }

    public String getInterestDbAndCollection() {
        return interestDbAndCollection;
    }

    public void setInterestDbAndCollection(String interestDbAndCollection) {
        this.interestDbAndCollection = interestDbAndCollection;
    }

    public String getMongoAddr() {
        return mongoAddr;
    }

    public void setMongoAddr(String mongoAddr) {
        this.mongoAddr = mongoAddr;
    }

    public String getMongoUserName() {
        return mongoUserName;
    }

    public void setMongoUserName(String mongoUserName) {
        this.mongoUserName = mongoUserName;
    }

    public String getMongoPassWord() {
        return mongoPassWord;
    }

    public void setMongoPassWord(String mongoPassWord) {
        this.mongoPassWord = mongoPassWord;
    }

    public String getDataSync() {
        return dataSync;
    }

    public void setDataSync(String dataSync) {
        this.dataSync = dataSync;
    }

    public String getReplicaSet() {
        return replicaSet;
    }

    public String getServerSelectionTimeoutMS() {
        return serverSelectionTimeoutMS;
    }

    public void setServerSelectionTimeoutMS(String serverSelectionTimeoutMS) {
        this.serverSelectionTimeoutMS = serverSelectionTimeoutMS;
    }

    public void setReplicaSet(String replicaSet) {
        this.replicaSet = replicaSet;
    }

    public String getConnectTimeoutMS() {
        return connectTimeoutMS;
    }

    public void setConnectTimeoutMS(String connectTimeoutMS) {
        this.connectTimeoutMS = connectTimeoutMS;
    }

    public String getSocketTimeoutMS() {
        return socketTimeoutMS;
    }

    public void setSocketTimeoutMS(String socketTimeoutMS) {
        this.socketTimeoutMS = socketTimeoutMS;
    }

    public String getSsl() {
        return ssl;
    }

    public void setSsl(String ssl) {
        this.ssl = ssl;
    }

    public String getTsl() {
        return tsl;
    }

    public void setTsl(String tsl) {
        this.tsl = tsl;
    }

    public String getTlsInsecure() {
        return tlsInsecure;
    }

    public void setTlsInsecure(String tlsInsecure) {
        this.tlsInsecure = tlsInsecure;
    }

    public String getSslInvalidHostNameAllowed() {
        return sslInvalidHostNameAllowed;
    }

    public void setSslInvalidHostNameAllowed(String sslInvalidHostNameAllowed) {
        this.sslInvalidHostNameAllowed = sslInvalidHostNameAllowed;
    }

    public String getTlsAllowInvalidHostnames() {
        return tlsAllowInvalidHostnames;
    }

    public void setTlsAllowInvalidHostnames(String tlsAllowInvalidHostnames) {
        this.tlsAllowInvalidHostnames = tlsAllowInvalidHostnames;
    }

    public String getCompressors() {
        return compressors;
    }

    public void setCompressors(String compressors) {
        this.compressors = compressors;
    }

    public void load(KeyValue props) {

        properties2Object(props, this);
    }

    private void properties2Object(final KeyValue p, final Object object) {

        Method[] methods = object.getClass().getMethods();
        for (Method method : methods) {
            String mn = method.getName();
            if (mn.startsWith("set")) {
                try {
                    String tmp = mn.substring(4);
                    String first = mn.substring(3, 4);

                    String key = first.toLowerCase() + tmp;
                    String property = p.getString(key);
                    if (property != null) {
                        Class<?>[] pt = method.getParameterTypes();
                        if (pt != null && pt.length > 0) {
                            String cn = pt[0].getSimpleName();
                            Object arg;
                            if (cn.equals("int") || cn.equals("Integer")) {
                                arg = Integer.parseInt(property);
                            } else if (cn.equals("long") || cn.equals("Long")) {
                                arg = Long.parseLong(property);
                            } else if (cn.equals("double") || cn.equals("Double")) {
                                arg = Double.parseDouble(property);
                            } else if (cn.equals("boolean") || cn.equals("Boolean")) {
                                arg = Boolean.parseBoolean(property);
                            } else if (cn.equals("float") || cn.equals("Float")) {
                                arg = Float.parseFloat(property);
                            } else if (cn.equals("String")) {
                                arg = property;
                            } else {
                                continue;
                            }
                            method.invoke(object, arg);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public BsonTimestamp getPosition() {
        return new BsonTimestamp(Integer.valueOf(positionTimeStamp), Integer.valueOf(positionInc));
    }

}
