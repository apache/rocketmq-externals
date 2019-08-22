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

package org.apache.connect.mongo;

import io.openmessaging.KeyValue;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.bson.BsonTimestamp;

public class SourceTaskConfig {

    private String mongoAddr;
    private String mongoUserName;
    private String mongoPassWord;
    private String interestDbAndCollection;
    private int positionTimeStamp;
    private int positionInc;
    private boolean dataSync;
    private long serverSelectionTimeoutMS;
    private long connectTimeoutMS;
    private long socketTimeoutMS;
    private boolean ssl;
    private boolean tsl;
    private boolean tlsInsecure;
    private boolean sslInvalidHostNameAllowed;
    private boolean tlsAllowInvalidHostnames;
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

    public String getInterestDbAndCollection() {
        return interestDbAndCollection;
    }

    public void setInterestDbAndCollection(String interestDbAndCollection) {
        this.interestDbAndCollection = interestDbAndCollection;
    }

    public int getPositionTimeStamp() {
        return positionTimeStamp;
    }

    public void setPositionTimeStamp(int positionTimeStamp) {
        this.positionTimeStamp = positionTimeStamp;
    }

    public int getPositionInc() {
        return positionInc;
    }

    public void setPositionInc(int positionInc) {
        this.positionInc = positionInc;
    }

    public boolean isDataSync() {
        return dataSync;
    }

    public void setDataSync(boolean dataSync) {
        this.dataSync = dataSync;
    }

    public long getServerSelectionTimeoutMS() {
        return serverSelectionTimeoutMS;
    }

    public void setServerSelectionTimeoutMS(long serverSelectionTimeoutMS) {
        this.serverSelectionTimeoutMS = serverSelectionTimeoutMS;
    }

    public long getConnectTimeoutMS() {
        return connectTimeoutMS;
    }

    public void setConnectTimeoutMS(long connectTimeoutMS) {
        this.connectTimeoutMS = connectTimeoutMS;
    }

    public long getSocketTimeoutMS() {
        return socketTimeoutMS;
    }

    public void setSocketTimeoutMS(long socketTimeoutMS) {
        this.socketTimeoutMS = socketTimeoutMS;
    }

    public boolean getSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public boolean getTsl() {
        return tsl;
    }

    public void setTsl(boolean tsl) {
        this.tsl = tsl;
    }

    public boolean getTlsInsecure() {
        return tlsInsecure;
    }

    public void setTlsInsecure(boolean tlsInsecure) {
        this.tlsInsecure = tlsInsecure;
    }

    public boolean getSslInvalidHostNameAllowed() {
        return sslInvalidHostNameAllowed;
    }

    public void setSslInvalidHostNameAllowed(boolean sslInvalidHostNameAllowed) {
        this.sslInvalidHostNameAllowed = sslInvalidHostNameAllowed;
    }

    public boolean getTlsAllowInvalidHostnames() {
        return tlsAllowInvalidHostnames;
    }

    public void setTlsAllowInvalidHostnames(boolean tlsAllowInvalidHostnames) {
        this.tlsAllowInvalidHostnames = tlsAllowInvalidHostnames;
    }

    public String getCompressors() {
        return compressors;
    }

    public void setCompressors(String compressors) {
        this.compressors = compressors;
    }

    public String getZlibCompressionLevel() {
        return zlibCompressionLevel;
    }

    public void setZlibCompressionLevel(String zlibCompressionLevel) {
        this.zlibCompressionLevel = zlibCompressionLevel;
    }

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

    public int getCopyThread() {
        return copyThread;
    }

    public void setCopyThread(int copyThread) {
        this.copyThread = copyThread;
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
