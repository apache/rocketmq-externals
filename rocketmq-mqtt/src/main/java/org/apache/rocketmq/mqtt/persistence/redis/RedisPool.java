package org.apache.rocketmq.mqtt.persistence.redis;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolAbstract;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

public class RedisPool {
    private JedisPoolAbstract pool;

    public RedisPool() {

    }

    public  RedisPool(MqttBridgeController mqttBridgeController) {

        RedisConfig redisConfig = mqttBridgeController.getMqttBridgeConfig().getRedisConfig();
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(redisConfig.getMaxTotal());
        config.setMaxIdle(redisConfig.getMaxIdle());
        config.setMinIdle(redisConfig.getMinIdle());
        config.setTestOnBorrow(redisConfig.isTestOnBorrow());
        config.setTestOnReturn(redisConfig.isTestOnReturn());

        if (redisConfig.isSentinelEnabled()) {
            Set<String> sentinels = Arrays.stream(redisConfig.getSentinelAddrs().split(";")).collect(Collectors.toSet());
            if (passworExists(redisConfig)) {
                pool = new JedisSentinelPool(redisConfig.getMasterName(),sentinels,config,redisConfig.getTimeout(),redisConfig.getPassword());
            }else {
                pool = new JedisSentinelPool(redisConfig.getMasterName(),sentinels,config,redisConfig.getTimeout());
            }
        }else{
            String[] ipAndPort = redisConfig.getRedisAddr().split(":");
            if(passworExists(redisConfig)){
                pool = new JedisPool(config,ipAndPort[0],Integer.parseInt(ipAndPort[1]),redisConfig.getTimeout(),redisConfig.getPassword());
            }else{
                pool = new JedisPool(config,ipAndPort[0],Integer.parseInt(ipAndPort[1]),redisConfig.getTimeout());
            }

        }
    }


    public  Jedis getJedis() {
        return pool.getResource();
    }

    public  void returnBrokenJedis(Jedis jedis) {
        try{
            Method method = pool.getClass().getDeclaredMethod("returnBrokenResource", Jedis.class);
            method.setAccessible(true);
            method.invoke(pool,jedis);
        } catch (Exception e){
            System.out.println(e);
        }
    }

    public  void returnJedis(Jedis jedis) {
        try{
            Method method = pool.getClass().getDeclaredMethod("returnResource", Jedis.class);
            method.setAccessible(true);
            method.invoke(pool,jedis);
        } catch (Exception e){
            System.out.println(e);
        }

    }
    private boolean passworExists(RedisConfig redisConfig){
        return redisConfig.getPassword() != null && !redisConfig.getPassword().equals("");
    }
}