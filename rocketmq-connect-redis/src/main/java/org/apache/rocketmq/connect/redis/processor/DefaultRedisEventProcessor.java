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

package org.apache.rocketmq.connect.redis.processor;

import com.moilioncircle.redis.replicator.RedisURI;
import com.moilioncircle.redis.replicator.cmd.Command;
import com.moilioncircle.redis.replicator.cmd.CommandName;
import com.moilioncircle.redis.replicator.cmd.CommandParser;
import com.moilioncircle.redis.replicator.cmd.parser.PingParser;
import com.moilioncircle.redis.replicator.cmd.parser.ReplConfParser;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import com.moilioncircle.redis.replicator.rdb.iterable.datatype.BatchedKeyValuePair;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.moilioncircle.redis.replicator.CloseListener;
import com.moilioncircle.redis.replicator.ExceptionListener;
import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.event.EventListener;
import org.apache.commons.lang.StringUtils;
import org.apache.rocketmq.connect.redis.common.Config;
import org.apache.rocketmq.connect.redis.common.RedisConstants;
import org.apache.rocketmq.connect.redis.common.SyncMod;
import org.apache.rocketmq.connect.redis.handler.RedisEventHandler;
import org.apache.rocketmq.connect.redis.parser.AppendParser;
import org.apache.rocketmq.connect.redis.parser.BitFieldParser;
import org.apache.rocketmq.connect.redis.parser.BitOpParser;
import org.apache.rocketmq.connect.redis.parser.BrPopLPushParser;
import org.apache.rocketmq.connect.redis.parser.DecrByParser;
import org.apache.rocketmq.connect.redis.parser.DecrParser;
import org.apache.rocketmq.connect.redis.parser.DelParser;
import org.apache.rocketmq.connect.redis.parser.EvalParser;
import org.apache.rocketmq.connect.redis.parser.EvalShaParser;
import org.apache.rocketmq.connect.redis.parser.ExecParser;
import org.apache.rocketmq.connect.redis.parser.ExpireAtParser;
import org.apache.rocketmq.connect.redis.parser.ExpireParser;
import org.apache.rocketmq.connect.redis.parser.FlushAllParser;
import org.apache.rocketmq.connect.redis.parser.FlushDbParser;
import org.apache.rocketmq.connect.redis.parser.GeoAddParser;
import org.apache.rocketmq.connect.redis.parser.GetsetParser;
import org.apache.rocketmq.connect.redis.parser.HDelParser;
import org.apache.rocketmq.connect.redis.parser.HIncrByParser;
import org.apache.rocketmq.connect.redis.parser.HSetNxParser;
import org.apache.rocketmq.connect.redis.parser.HSetParser;
import org.apache.rocketmq.connect.redis.parser.HmSetParser;
import org.apache.rocketmq.connect.redis.parser.IncrByParser;
import org.apache.rocketmq.connect.redis.parser.IncrParser;
import org.apache.rocketmq.connect.redis.parser.LPopParser;
import org.apache.rocketmq.connect.redis.parser.LPushParser;
import org.apache.rocketmq.connect.redis.parser.LPushXParser;
import org.apache.rocketmq.connect.redis.parser.LRemParser;
import org.apache.rocketmq.connect.redis.parser.LSetParser;
import org.apache.rocketmq.connect.redis.parser.LTrimParser;
import org.apache.rocketmq.connect.redis.parser.LinsertParser;
import org.apache.rocketmq.connect.redis.parser.MSetNxParser;
import org.apache.rocketmq.connect.redis.parser.MSetParser;
import org.apache.rocketmq.connect.redis.parser.MoveParser;
import org.apache.rocketmq.connect.redis.parser.MultiParser;
import org.apache.rocketmq.connect.redis.parser.PExpireAtParser;
import org.apache.rocketmq.connect.redis.parser.PExpireParser;
import org.apache.rocketmq.connect.redis.parser.PSetExParser;
import org.apache.rocketmq.connect.redis.parser.PersistParser;
import org.apache.rocketmq.connect.redis.parser.PfAddParser;
import org.apache.rocketmq.connect.redis.parser.PfCountParser;
import org.apache.rocketmq.connect.redis.parser.PfMergeParser;
import org.apache.rocketmq.connect.redis.parser.PublishParser;
import org.apache.rocketmq.connect.redis.parser.RPopLPushParser;
import org.apache.rocketmq.connect.redis.parser.RPopParser;
import org.apache.rocketmq.connect.redis.parser.RPushParser;
import org.apache.rocketmq.connect.redis.parser.RPushXParser;
import org.apache.rocketmq.connect.redis.parser.RenameNxParser;
import org.apache.rocketmq.connect.redis.parser.RenameParser;
import org.apache.rocketmq.connect.redis.parser.RestoreParser;
import org.apache.rocketmq.connect.redis.parser.SAddParser;
import org.apache.rocketmq.connect.redis.parser.SDiffStoreParser;
import org.apache.rocketmq.connect.redis.parser.SInterStoreParser;
import org.apache.rocketmq.connect.redis.parser.SMoveParser;
import org.apache.rocketmq.connect.redis.parser.SRemParser;
import org.apache.rocketmq.connect.redis.parser.SUnionStoreParser;
import org.apache.rocketmq.connect.redis.parser.ScriptParser;
import org.apache.rocketmq.connect.redis.parser.SelectParser;
import org.apache.rocketmq.connect.redis.parser.SetBitParser;
import org.apache.rocketmq.connect.redis.parser.SetExParser;
import org.apache.rocketmq.connect.redis.parser.SetNxParser;
import org.apache.rocketmq.connect.redis.parser.SetParser;
import org.apache.rocketmq.connect.redis.parser.SetRangeParser;
import org.apache.rocketmq.connect.redis.parser.SortParser;
import org.apache.rocketmq.connect.redis.parser.SwapDbParser;
import org.apache.rocketmq.connect.redis.parser.UnLinkParser;
import org.apache.rocketmq.connect.redis.parser.XAckParser;
import org.apache.rocketmq.connect.redis.parser.XAddParser;
import org.apache.rocketmq.connect.redis.parser.XClaimParser;
import org.apache.rocketmq.connect.redis.parser.XDelParser;
import org.apache.rocketmq.connect.redis.parser.XGroupParser;
import org.apache.rocketmq.connect.redis.parser.XSetIdParser;
import org.apache.rocketmq.connect.redis.parser.XTrimParser;
import org.apache.rocketmq.connect.redis.parser.ZAddParser;
import org.apache.rocketmq.connect.redis.parser.ZIncrByParser;
import org.apache.rocketmq.connect.redis.parser.ZInterStoreParser;
import org.apache.rocketmq.connect.redis.parser.ZPopMaxParser;
import org.apache.rocketmq.connect.redis.parser.ZPopMinParser;
import org.apache.rocketmq.connect.redis.parser.ZRemParser;
import org.apache.rocketmq.connect.redis.parser.ZRemRangeByLexParser;
import org.apache.rocketmq.connect.redis.parser.ZRemRangeByRankParser;
import org.apache.rocketmq.connect.redis.parser.ZRemRangeByScoreParser;
import org.apache.rocketmq.connect.redis.parser.ZUnionStoreParser;
import org.apache.rocketmq.connect.redis.pojo.KVEntry;
import org.apache.rocketmq.connect.redis.pojo.RedisEvent;
import org.apache.rocketmq.connect.redis.util.ParseStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * listen redis event
 */
public class DefaultRedisEventProcessor implements RedisEventProcessor {
    protected final Logger LOGGER = LoggerFactory.getLogger(DefaultRedisEventProcessor.class);
    /**
     * redis event cache.
     */
    protected BlockingQueue<RedisEvent> eventQueue = new LinkedBlockingQueue(50000);
    protected Config config;
    private volatile AtomicBoolean stop = new AtomicBoolean(true);
    /**
     * handle different kind of redis event.
     */
    private RedisEventHandler redisEventHandler;
    private JedisPool pool;
    /**
     * redis info msg cache.
     */
    private Map<String, String> redisInfo;
    /**
     * redis event parsers cache.
     */
    private ParserCache parserCache;

    /**
     * redis replicator
     */
    private Replicator replicator;
    private final EventListener eventListener;
    private final ExceptionListener exceptionListener;
    private final CloseListener closeListener;

    private List<RedisEventProcessorCallback> redisEventProcessorCallbacks = new CopyOnWriteArrayList<>();

    private final Integer pollTimeout = 1000;
    private final Integer offerTimeout = 1000;
    /**
     * base construct
     *
     * @param config
     */
    public DefaultRedisEventProcessor(Config config) {
        this.config = config;
        this.pool = getJedisPool(config);

        this.parserCache = new ParserCache();
        this.closeListener = new RedisClosedListener(this);
        this.exceptionListener = new RedisExceptionListener(this);
        this.eventListener = new RedisEventListener(this.config,this);
    }

    @Override public void registEventHandler(RedisEventHandler eventHandler) {
        this.redisEventHandler = eventHandler;
    }

    @Override public void registProcessorCallback(RedisEventProcessorCallback redisEventProcessorCallback){
        redisEventProcessorCallbacks.add(redisEventProcessorCallback);
    }

    /**
     * start redis replicator and asynchronous processing threads
     */
    @Override public void start() throws IllegalStateException, IOException {
        if (this.stop.compareAndSet(true, false)) {
            Jedis jedis = this.pool.getResource();
            String jedisInfo = jedis.info(RedisConstants.REDIS_INFO_REPLICATION);
            this.redisInfo = ParseStringUtils.parseRedisInfo2Map(jedisInfo);
            String replId = this.redisInfo.get(RedisConstants.REDIS_INFO_REPLICATION_MASTER_REPLID);
            if (StringUtils.isNotEmpty(replId)
                && StringUtils.isEmpty(this.config.getReplId())) {
                this.config.setReplId(replId);
            }
            String offset = this.redisInfo.get(RedisConstants.REDIS_INFO_REPLICATION_MASTER_REPL_OFFSET);
            // 如果是LAST_OFFSET，则将offset设置为当前Redis最新的offset值。
            // LAST_OFFSET、CUSTOM_OFFSET，优先使用connector runtime中的存储位点信息。
            if (SyncMod.LAST_OFFSET.equals(this.config.getSyncMod())) {
                if (this.config.getPosition() != null) {
                    this.config.setOffset(this.config.getPosition());
                } else if (StringUtils.isNotBlank(offset)) {
                    this.config.setOffset(Long.parseLong(offset));
                }
            } else if(SyncMod.CUSTOM_OFFSET.equals(this.config.getSyncMod())){
                if(this.config.getPosition() != null){
                    this.config.setOffset(this.config.getPosition());
                }
            }

            startReplicatorAsync(this.config.getReplId(), this.config.getOffset());
            LOGGER.info("processor start from replId: {}, offset: {}", this.config.getReplId(), this.config.getOffset());
        } else {
            LOGGER.warn("processor is already started.");
        }
    }

    @Override public void stop() throws IOException {
        if (this.stop.compareAndSet(false, true)) {
            if (this.replicator != null) {
                this.replicator.close();
            }
            this.pool.close();
            int size = redisEventProcessorCallbacks.size();
            for (int i = 0; i < size; i++) {
                redisEventProcessorCallbacks.get(i).onStop(this);
            }
            LOGGER.info("processor is stopped.");
        } else {
            LOGGER.info("processor is already stopped.");
        }
    }


    @Override public boolean commit(RedisEvent event) throws Exception {
        return this.eventQueue.offer(event, this.offerTimeout, TimeUnit.MILLISECONDS);
    }


    @Override public KVEntry poll() throws Exception {
        RedisEvent event = this.eventQueue.poll(this.pollTimeout, TimeUnit.MILLISECONDS);
        if (event == null) {
            return null;
        }
        if (event.getEvent() instanceof KeyValuePair) {
            if (event.getEvent() instanceof BatchedKeyValuePair) {
                return redisEventHandler.handleBatchKVString(event.getReplId(), event.getReplOffset(),
                    (BatchedKeyValuePair) event.getEvent());
            } else {
                return redisEventHandler.handleKVString(event.getReplId(), event.getReplOffset(),
                    (KeyValuePair) event.getEvent());
            }
        } else if (event.getEvent() instanceof Command) {
            return redisEventHandler.handleCommand(event.getReplId(), event.getReplOffset(), (Command) event.getEvent());
        } else {
            return redisEventHandler.handleOtherEvent(event.getReplId(), event.getReplOffset(), event.getEvent());
        }
    }


    @Override public boolean isStopped() {
        return this.stop.get();
    }

    /**
     * start redis replicator async
     *
     * @throws IOException
     */
    private void startReplicatorAsync(String replId, Long offset) throws IllegalStateException, IOException {
        RedisURI uri = this.config.getRedisUri();
        if (uri == null) {
            throw new IllegalStateException("redis uri error.");
        }
        this.replicator = new com.moilioncircle.redis.replicator.RedisReplicator(uri);
        this.dress(this.replicator);
        // set listeners
        this.replicator.addEventListener(eventListener);
        this.replicator.addExceptionListener(exceptionListener);
        this.replicator.addCloseListener(closeListener);

        if (this.config.getReplId() != null) {
            this.replicator.getConfiguration().setReplId(replId);
        }
        if (this.config.getOffset() != null && this.config.getOffset() >= -1) {
            this.replicator.getConfiguration().setReplOffset(offset);
        }
        new Thread(() -> {
            try {
                this.replicator.open();
            } catch (IOException e) {
                LOGGER.error("start replicator error. {}", e);
                try {
                    this.stop();
                } catch (IOException ie) {
                }
            }
        }).start();
    }

    private JedisPool getJedisPool(Config config) {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(100);
        jedisPoolConfig.setMaxIdle(50);
        jedisPoolConfig.setMaxWaitMillis(3000);
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPoolConfig.setTestOnReturn(true);
        String pwd = null;
        if (StringUtils.isNotBlank(config.getRedisPassword())) {
            pwd = config.getRedisPassword();
        }

        return new JedisPool(jedisPoolConfig,
            config.getRedisAddr(),
            config.getRedisPort(),
            config.getTimeout(),
            pwd);
    }

    /**
     * 添加用户未指定的Redis指令对应的parser
     *
     * @param r
     * @return
     */
    private Replicator dress(Replicator r) {
        String commands = this.config.getCommands();
        if (!StringUtils.isEmpty(commands) && !RedisConstants.ALL_COMMAND.equals(commands)) {
            List<String> commandList = ParseStringUtils.parseCommands(commands);
            if (commandList != null) {
                int size = commandList.size();
                for (int i = 0; i < size; i++) {
                    CommandName commandName = CommandName.name(commandList.get(i));
                    if (parserCache.containsKey(commandName)) {
                        r.addCommandParser(commandName, parserCache.get(commandName));
                    }
                }
            }
        }
        for (Map.Entry<CommandName, CommandParser> entry : parserCache.entrySet()) {
            r.addCommandParser(entry.getKey(), entry.getValue());
        }
        return r;
    }

    private class ParserCache extends HashMap<CommandName, CommandParser> {
        public ParserCache() {
            init();
        }

        public void init() {
            put(CommandName.name("PING"), new PingParser());
            put(CommandName.name("REPLCONF"), new ReplConfParser());
            put(CommandName.name("APPEND"), new AppendParser());
            put(CommandName.name("SET"), new SetParser());
            put(CommandName.name("SETEX"), new SetExParser());
            put(CommandName.name("MSET"), new MSetParser());
            put(CommandName.name("DEL"), new DelParser());
            put(CommandName.name("SADD"), new SAddParser());
            put(CommandName.name("HMSET"), new HmSetParser());
            put(CommandName.name("HSET"), new HSetParser());
            put(CommandName.name("LSET"), new LSetParser());
            put(CommandName.name("EXPIRE"), new ExpireParser());
            put(CommandName.name("EXPIREAT"), new ExpireAtParser());
            put(CommandName.name("GETSET"), new GetsetParser());
            put(CommandName.name("HSETNX"), new HSetNxParser());
            put(CommandName.name("MSETNX"), new MSetNxParser());
            put(CommandName.name("PSETEX"), new PSetExParser());
            put(CommandName.name("SETNX"), new SetNxParser());
            put(CommandName.name("SETRANGE"), new SetRangeParser());
            put(CommandName.name("HDEL"), new HDelParser());
            put(CommandName.name("LPOP"), new LPopParser());
            put(CommandName.name("LPUSH"), new LPushParser());
            put(CommandName.name("LPUSHX"), new LPushXParser());
            put(CommandName.name("LRem"), new LRemParser());
            put(CommandName.name("RPOP"), new RPopParser());
            put(CommandName.name("RPUSH"), new RPushParser());
            put(CommandName.name("RPUSHX"), new RPushXParser());
            put(CommandName.name("ZREM"), new ZRemParser());
            put(CommandName.name("RENAME"), new RenameParser());
            put(CommandName.name("INCR"), new IncrParser());
            put(CommandName.name("DECR"), new DecrParser());
            put(CommandName.name("INCRBY"), new IncrByParser());
            put(CommandName.name("DECRBY"), new DecrByParser());
            put(CommandName.name("PERSIST"), new PersistParser());
            put(CommandName.name("SELECT"), new SelectParser());
            put(CommandName.name("FLUSHALL"), new FlushAllParser());
            put(CommandName.name("FLUSHDB"), new FlushDbParser());
            put(CommandName.name("HINCRBY"), new HIncrByParser());
            put(CommandName.name("ZINCRBY"), new ZIncrByParser());
            put(CommandName.name("MOVE"), new MoveParser());
            put(CommandName.name("SMOVE"), new SMoveParser());
            put(CommandName.name("PFADD"), new PfAddParser());
            put(CommandName.name("PFCOUNT"), new PfCountParser());
            put(CommandName.name("PFMERGE"), new PfMergeParser());
            put(CommandName.name("SDIFFSTORE"), new SDiffStoreParser());
            put(CommandName.name("SINTERSTORE"), new SInterStoreParser());
            put(CommandName.name("SUNIONSTORE"), new SUnionStoreParser());
            put(CommandName.name("ZADD"), new ZAddParser());
            put(CommandName.name("ZINTERSTORE"), new ZInterStoreParser());
            put(CommandName.name("ZUNIONSTORE"), new ZUnionStoreParser());
            put(CommandName.name("BRPOPLPUSH"), new BrPopLPushParser());
            put(CommandName.name("LINSERT"), new LinsertParser());
            put(CommandName.name("RENAMENX"), new RenameNxParser());
            put(CommandName.name("RESTORE"), new RestoreParser());
            put(CommandName.name("PEXPIRE"), new PExpireParser());
            put(CommandName.name("PEXPIREAT"), new PExpireAtParser());
            put(CommandName.name("GEOADD"), new GeoAddParser());
            put(CommandName.name("EVAL"), new EvalParser());
            put(CommandName.name("EVALSHA"), new EvalShaParser());
            put(CommandName.name("SCRIPT"), new ScriptParser());
            put(CommandName.name("PUBLISH"), new PublishParser());
            put(CommandName.name("BITOP"), new BitOpParser());
            put(CommandName.name("BITFIELD"), new BitFieldParser());
            put(CommandName.name("SETBIT"), new SetBitParser());
            put(CommandName.name("SREM"), new SRemParser());
            put(CommandName.name("UNLINK"), new UnLinkParser());
            put(CommandName.name("SWAPDB"), new SwapDbParser());
            put(CommandName.name("MULTI"), new MultiParser());
            put(CommandName.name("EXEC"), new ExecParser());
            put(CommandName.name("ZREMRANGEBYSCORE"), new ZRemRangeByScoreParser());
            put(CommandName.name("ZREMRANGEBYRANK"), new ZRemRangeByRankParser());
            put(CommandName.name("ZREMRANGEBYLEX"), new ZRemRangeByLexParser());
            put(CommandName.name("LTRIM"), new LTrimParser());
            put(CommandName.name("SORT"), new SortParser());
            put(CommandName.name("RPOPLPUSH"), new RPopLPushParser());
            put(CommandName.name("ZPOPMIN"), new ZPopMinParser());
            put(CommandName.name("ZPOPMAX"), new ZPopMaxParser());
            put(CommandName.name("XACK"), new XAckParser());
            put(CommandName.name("XADD"), new XAddParser());
            put(CommandName.name("XCLAIM"), new XClaimParser());
            put(CommandName.name("XDEL"), new XDelParser());
            put(CommandName.name("XGROUP"), new XGroupParser());
            put(CommandName.name("XTRIM"), new XTrimParser());
            put(CommandName.name("XSETID"), new XSetIdParser());
        }
    }
}
