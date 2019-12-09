package org.apache.rocketmq.mqtt.config;

import org.apache.rocketmq.remoting.netty.NettyServerConfig;
import org.apache.rocketmq.remoting.netty.NettySystemConfig;

public class MultiNettyServerConfig extends NettyServerConfig {

    private int mqttListenPort = 1883;
    private int mqttServerWorkerThreads = 8;
    private int mqttServerCallbackExecutorThreads = 0;
    private int mqttServerSelectorThreads = 3;
    private int mqttServerOnewaySemaphoreValue = 256;
    private int mqttServerAsyncSemaphoreValue = 64;
    private int mqttServerChannelMaxIdleTimeSeconds = 90;

    private int mqttServerSocketSndBufSize = NettySystemConfig.socketSndbufSize;
    private int mqttServerSocketRcvBufSize = NettySystemConfig.socketRcvbufSize;
    private boolean mqttServerPooledByteBufAllocatorEnable = true;
    private int mqttServerAcceptorThreads = 1;
    private int mqttConnectionChannelReaderIdleSeconds = 0;
    private int mqttConnectionChannelWriterIdleSeconds = 0;

    private int maxBytesInMqttMessage = 4 * 1024 * 1024;

    public int getMqttListenPort() {
        return mqttListenPort;
    }

    public void setMqttListenPort(int mqttListenPort) {
        this.mqttListenPort = mqttListenPort;
    }

    public int getMqttServerWorkerThreads() {
        return mqttServerWorkerThreads;
    }

    public void setMqttServerWorkerThreads(int mqttServerWorkerThreads) {
        this.mqttServerWorkerThreads = mqttServerWorkerThreads;
    }

    public int getMqttServerCallbackExecutorThreads() {
        return mqttServerCallbackExecutorThreads;
    }

    public void setMqttServerCallbackExecutorThreads(int mqttServerCallbackExecutorThreads) {
        this.mqttServerCallbackExecutorThreads = mqttServerCallbackExecutorThreads;
    }

    public int getMqttServerSelectorThreads() {
        return mqttServerSelectorThreads;
    }

    public void setMqttServerSelectorThreads(int mqttServerSelectorThreads) {
        this.mqttServerSelectorThreads = mqttServerSelectorThreads;
    }

    public int getMqttServerOnewaySemaphoreValue() {
        return mqttServerOnewaySemaphoreValue;
    }

    public void setMqttServerOnewaySemaphoreValue(int mqttServerOnewaySemaphoreValue) {
        this.mqttServerOnewaySemaphoreValue = mqttServerOnewaySemaphoreValue;
    }

    public int getMqttServerAsyncSemaphoreValue() {
        return mqttServerAsyncSemaphoreValue;
    }

    public void setMqttServerAsyncSemaphoreValue(int mqttServerAsyncSemaphoreValue) {
        this.mqttServerAsyncSemaphoreValue = mqttServerAsyncSemaphoreValue;
    }

    public int getMqttServerChannelMaxIdleTimeSeconds() {
        return mqttServerChannelMaxIdleTimeSeconds;
    }

    public void setMqttServerChannelMaxIdleTimeSeconds(int mqttServerChannelMaxIdleTimeSeconds) {
        this.mqttServerChannelMaxIdleTimeSeconds = mqttServerChannelMaxIdleTimeSeconds;
    }

    public int getMqttServerSocketSndBufSize() {
        return mqttServerSocketSndBufSize;
    }

    public void setMqttServerSocketSndBufSize(int mqttServerSocketSndBufSize) {
        this.mqttServerSocketSndBufSize = mqttServerSocketSndBufSize;
    }

    public int getMqttServerSocketRcvBufSize() {
        return mqttServerSocketRcvBufSize;
    }

    public void setMqttServerSocketRcvBufSize(int mqttServerSocketRcvBufSize) {
        this.mqttServerSocketRcvBufSize = mqttServerSocketRcvBufSize;
    }

    public boolean isMqttServerPooledByteBufAllocatorEnable() {
        return mqttServerPooledByteBufAllocatorEnable;
    }

    public void setMqttServerPooledByteBufAllocatorEnable(boolean mqttServerPooledByteBufAllocatorEnable) {
        this.mqttServerPooledByteBufAllocatorEnable = mqttServerPooledByteBufAllocatorEnable;
    }

    public int getMqttServerAcceptorThreads() {
        return mqttServerAcceptorThreads;
    }

    public void setMqttServerAcceptorThreads(int mqttServerAcceptorThreads) {
        this.mqttServerAcceptorThreads = mqttServerAcceptorThreads;
    }

    public int getMqttConnectionChannelReaderIdleSeconds() {
        return mqttConnectionChannelReaderIdleSeconds;
    }

    public void setMqttConnectionChannelReaderIdleSeconds(int mqttConnectionChannelReaderIdleSeconds) {
        this.mqttConnectionChannelReaderIdleSeconds = mqttConnectionChannelReaderIdleSeconds;
    }

    public int getMqttConnectionChannelWriterIdleSeconds() {
        return mqttConnectionChannelWriterIdleSeconds;
    }

    public void setMqttConnectionChannelWriterIdleSeconds(int mqttConnectionChannelWriterIdleSeconds) {
        this.mqttConnectionChannelWriterIdleSeconds = mqttConnectionChannelWriterIdleSeconds;
    }

    public int getMaxBytesInMqttMessage() {
        return maxBytesInMqttMessage;
    }

    public void setMaxBytesInMqttMessage(int maxBytesInMqttMessage) {
        this.maxBytesInMqttMessage = maxBytesInMqttMessage;
    }
}
