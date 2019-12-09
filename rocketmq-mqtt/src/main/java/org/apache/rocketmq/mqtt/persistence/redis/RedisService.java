package org.apache.rocketmq.mqtt.persistence.redis;

import java.util.Set;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

public class RedisService {
    private static final Logger log = LoggerFactory.getLogger(RedisService.class);
    private RedisPool redisPool;

    public RedisService(RedisPool redisPool) {
        this.redisPool = redisPool;
    }

    /**
     * 设置key的有效期，单位是秒
     *
     * @param key
     * @param exTime
     * @return
     */
    public Long expire(String key, int exTime) {
        Jedis jedis = null;
        Long result = null;
        try {
            jedis = redisPool.getJedis();
            result = jedis.expire(key, exTime);
        } catch (Exception e) {
            log.error("expire key:{} error", key, e);
            redisPool.returnBrokenJedis(jedis);
            return result;
        }
        redisPool.returnJedis(jedis);
        return result;
    }

    //exTime的单位是秒
    public String setEx(String key, String value, int exTime) {
        Jedis jedis = null;
        String result = null;
        try {
            jedis = redisPool.getJedis();
            result = jedis.setex(key, exTime, value);
        } catch (Exception e) {
            log.error("setex key:{} value:{} error", key, value, e);
            redisPool.returnBrokenJedis(jedis);
            return result;
        }
        redisPool.returnJedis(jedis);
        return result;
    }

    public String set(String key, String value) {
        Jedis jedis = null;
        String result = null;

        try {
            jedis = redisPool.getJedis();
            result = jedis.set(key, value);
        } catch (Exception e) {
            log.error("set key:{} value:{} error", key, value, e);
            redisPool.returnBrokenJedis(jedis);
            return result;
        }
        redisPool.returnJedis(jedis);
        return result;
    }

    public String get(String key) {
        Jedis jedis = null;
        String result = null;
        try {
            jedis = redisPool.getJedis();
            result = jedis.get(key);
        } catch (Exception e) {
            log.error("get key:{} error", key, e);
            redisPool.returnBrokenJedis(jedis);
            return result;
        }
        redisPool.returnJedis(jedis);
        return result;
    }

    public long del(String key) {
        Jedis jedis = null;
        long result = 0;
        try {
            jedis = redisPool.getJedis();
            result = jedis.del(key);
        } catch (Exception e) {
            log.error("del key:{} error", key, e);
            redisPool.returnBrokenJedis(jedis);
            return result;
        }
        redisPool.returnJedis(jedis);
        return result;
    }

    public long addSetMember(String key, String... member) {
        Jedis jedis = null;
        long result = 0;
        try {
            jedis = redisPool.getJedis();
            result = jedis.sadd(key, member);
        } catch (Exception e) {
            log.error("addSetMember key:{} member:{} error", key,member, e);
            redisPool.returnBrokenJedis(jedis);
            return result;
        }
        redisPool.returnJedis(jedis);
        return result;
    }
    public long deleteSetMember(String key, String... member) {
        Jedis jedis = null;
        long result = 0;
        try {
            jedis = redisPool.getJedis();
            result = jedis.srem(key, member);
        } catch (Exception e) {
            log.error("deleteSetMember key:{} member:{} error", key,member, e);
            redisPool.returnBrokenJedis(jedis);
            return result;
        }
        redisPool.returnJedis(jedis);
        return result;
    }
    public Set<String> getAllMember(String key) {
        Jedis jedis = null;
        Set<String> result = null;
        try {
            jedis = redisPool.getJedis();
            result = jedis.smembers(key);
        } catch (Exception e) {
            log.error("getAllMember key:{} error", key, e);
            redisPool.returnBrokenJedis(jedis);
            return result;
        }
        redisPool.returnJedis(jedis);
        return result;
    }
}
