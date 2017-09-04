/*
 *
 *   Copyright 2016 leon chen
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  modified:
 *    1. rename package from com.moilioncircle.redis.replicator to
 *        org.apache.rocketmq.replicator.redis
 *
 */

package org.apache.rocketmq.replicator.redis;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class Configuration {

    private Configuration() {
    }

    /**
     * factory
     *
     * @return Configuration
     */
    public static Configuration defaultSetting() {
        return new Configuration();
    }

    /**
     * socket connection timeout
     */
    private int connectionTimeout = 30000;

    /**
     * socket input stream read timeout
     */
    private int readTimeout = 30000;

    /**
     * socket receive buffer size
     */
    private int receiveBufferSize = 0;

    /**
     * socket send buffer size
     */
    private int sendBufferSize = 0;

    /**
     * connection retry times. if retries <= 0 then always retry
     */
    private int retries = 5;

    /**
     * retry time interval
     */
    private int retryTimeInterval = 1000;

    /**
     * redis input stream buffer size
     */
    private int bufferSize = 8 * 1024;

    /**
     * auth password
     */
    private String authPassword = null;

    /**
     * discard rdb parser
     */
    private boolean discardRdbEvent = false;

    /**
     * async cached bytes
     */
    private int asyncCachedBytes = 512 * 1024;

    /**
     * trace event log
     */
    private boolean verbose = false;

    /**
     * used in psync heart beat
     */
    private int heartBeatPeriod = 200;

    /**
     * open ssl connection
     */
    private boolean ssl = false;

    /**
     * ssl socket factory
     */
    private SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

    /**
     * ssl parameters
     */
    private SSLParameters sslParameters;

    /**
     * hostname verifier
     */
    private HostnameVerifier hostnameVerifier;

    /**
     * psync master run id
     */
    private String replId = "?";

    /**
     * psync offset
     */
    private final AtomicLong replOffset = new AtomicLong(-1);

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public Configuration setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public Configuration setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public int getRetries() {
        return retries;
    }

    public Configuration setRetries(int retries) {
        this.retries = retries;
        return this;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public Configuration setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
        return this;
    }

    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    public Configuration setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
        return this;
    }

    public int getSendBufferSize() {
        return sendBufferSize;
    }

    public Configuration setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
        return this;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public Configuration setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    public boolean isDiscardRdbEvent() {
        return discardRdbEvent;
    }

    public Configuration setDiscardRdbEvent(boolean discardRdbEvent) {
        this.discardRdbEvent = discardRdbEvent;
        return this;
    }

    public String getReplId() {
        return replId;
    }

    public Configuration setReplId(String replId) {
        this.replId = replId;
        return this;
    }

    public long getReplOffset() {
        return replOffset.get();
    }

    public Configuration setReplOffset(long replOffset) {
        this.replOffset.set(replOffset);
        return this;
    }

    public Configuration addOffset(long offset) {
        this.replOffset.addAndGet(offset);
        return this;
    }

    public int getAsyncCachedBytes() {
        return asyncCachedBytes;
    }

    public Configuration setAsyncCachedBytes(int asyncCachedBytes) {
        this.asyncCachedBytes = asyncCachedBytes;
        return this;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public Configuration setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    public int getHeartBeatPeriod() {
        return heartBeatPeriod;
    }

    public Configuration setHeartBeatPeriod(int heartBeatPeriod) {
        this.heartBeatPeriod = heartBeatPeriod;
        return this;
    }

    public int getRetryTimeInterval() {
        return retryTimeInterval;
    }

    public Configuration setRetryTimeInterval(int retryTimeInterval) {
        this.retryTimeInterval = retryTimeInterval;
        return this;
    }

    public boolean isSsl() {
        return ssl;
    }

    public Configuration setSsl(boolean ssl) {
        this.ssl = ssl;
        return this;
    }

    public SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    public Configuration setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
        return this;
    }

    public SSLParameters getSslParameters() {
        return sslParameters;
    }

    public Configuration setSslParameters(SSLParameters sslParameters) {
        this.sslParameters = sslParameters;
        return this;
    }

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public Configuration setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
        return this;
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "connectionTimeout=" + connectionTimeout +
                ", readTimeout=" + readTimeout +
                ", receiveBufferSize=" + receiveBufferSize +
                ", sendBufferSize=" + sendBufferSize +
                ", retries=" + retries +
                ", retryTimeInterval=" + retryTimeInterval +
                ", bufferSize=" + bufferSize +
                ", authPassword='" + authPassword + '\'' +
                ", discardRdbEvent=" + discardRdbEvent +
                ", asyncCachedBytes=" + asyncCachedBytes +
                ", verbose=" + verbose +
                ", heartBeatPeriod=" + heartBeatPeriod +
                ", ssl=" + ssl +
                ", sslSocketFactory=" + sslSocketFactory +
                ", sslParameters=" + sslParameters +
                ", hostnameVerifier=" + hostnameVerifier +
                ", replId='" + replId + '\'' +
                ", replOffset=" + replOffset +
                '}';
    }
}
