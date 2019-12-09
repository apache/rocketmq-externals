package org.apache.rocketmq.mqtt.persistence.redis;

public class RedisConfig {
    private boolean sentinelEnabled = false;
    private String sentinelAddrs = "127.0.0.1:26379";
    private String masterName="mymaster";
    private String redisAddr = "127.0.0.1:6379";
    private int maxTotal = 8;
    private int maxIdle = 8;
    private int minIdle = 0;
    private boolean testOnBorrow = true;
    private boolean testOnReturn = true;
    private String password;
    private int timeout = 3000;

    public boolean isSentinelEnabled() {
        return sentinelEnabled;
    }

    public void setSentinelEnabled(boolean sentinelEnabled) {
        this.sentinelEnabled = sentinelEnabled;
    }

    public String getSentinelAddrs() {
        return sentinelAddrs;
    }

    public void setSentinelAddrs(String sentinelAddrs) {
        this.sentinelAddrs = sentinelAddrs;
    }

    public String getRedisAddr() {
        return redisAddr;
    }

    public void setRedisAddr(String redisAddr) {
        this.redisAddr = redisAddr;
    }

    public int getMaxTotal() {
        return maxTotal;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public boolean isTestOnBorrow() {
        return testOnBorrow;
    }

    public void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    public boolean isTestOnReturn() {
        return testOnReturn;
    }

    public void setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMasterName() {
        return masterName;
    }

    public void setMasterName(String masterName) {
        this.masterName = masterName;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
