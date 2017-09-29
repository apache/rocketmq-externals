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

package org.apache.rocketmq.redis.replicator;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.rocketmq.redis.replicator.cmd.Command;
import org.apache.rocketmq.redis.replicator.cmd.CommandName;
import org.apache.rocketmq.redis.replicator.cmd.CommandParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.AppendParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.BRPopLPushParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.BitOpParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.DecrByParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.DecrParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.DelParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.EvalParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.ExecParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.ExpireAtParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.ExpireParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.FlushAllParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.FlushDBParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.GeoAddParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.GetSetParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.HDelParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.HIncrByParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.HMSetParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.HSetNxParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.HSetParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.IncrByParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.IncrParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.LInsertParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.LPushParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.LPushXParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.LRemParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.MSetNxParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.MultiParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.PExpireAtParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.PExpireParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.PFAddParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.PFCountParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.PFMergeParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.PSetExParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.PersistParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.PingParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.PublishParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.RPopLPushParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.RPushParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.RPushXParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.RenameNxParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.RenameParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.RestoreParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.SAddParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.SDiffStoreParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.SInterStoreParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.SMoveParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.SRemParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.SUnionStoreParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.ScriptParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.SelectParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.SetBitParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.SetExParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.SetNxParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.SortParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.SwapDBParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.UnLinkParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.ZAddParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.ZIncrByParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.ZInterStoreParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.ZRemParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.ZUnionStoreParser;
import org.apache.rocketmq.redis.replicator.event.Event;
import org.apache.rocketmq.redis.replicator.io.RedisInputStream;
import org.apache.rocketmq.redis.replicator.rdb.RdbVisitor;
import org.apache.rocketmq.redis.replicator.rdb.datatype.AuxField;
import org.apache.rocketmq.redis.replicator.rdb.datatype.KeyValuePair;
import org.apache.rocketmq.redis.replicator.rdb.module.ModuleKey;
import org.apache.rocketmq.redis.replicator.rdb.module.ModuleParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.BitFieldParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.LPopParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.LSetParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.LTrimParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.MSetParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.MoveParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.RPopParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.SetParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.SetRangeParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.ZRemRangeByLexParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.ZRemRangeByRankParser;
import org.apache.rocketmq.redis.replicator.cmd.parser.ZRemRangeByScoreParser;
import org.apache.rocketmq.redis.replicator.event.PostFullSyncEvent;
import org.apache.rocketmq.redis.replicator.event.PreFullSyncEvent;
import org.apache.rocketmq.redis.replicator.rdb.DefaultRdbVisitor;
import org.apache.rocketmq.redis.replicator.rdb.datatype.Module;

import static org.apache.rocketmq.redis.replicator.Status.CONNECTED;
import static org.apache.rocketmq.redis.replicator.Status.DISCONNECTED;
import static org.apache.rocketmq.redis.replicator.Status.DISCONNECTING;

public abstract class AbstractReplicator extends AbstractReplicatorListener implements Replicator {
    protected Configuration configuration;
    protected volatile RedisInputStream inputStream;
    protected RdbVisitor rdbVisitor = new DefaultRdbVisitor(this);
    protected final AtomicReference<Status> connected = new AtomicReference<>(DISCONNECTED);
    protected final Map<ModuleKey, ModuleParser<? extends Module>> modules = new ConcurrentHashMap<>();
    protected final Map<CommandName, CommandParser<? extends Command>> commands = new ConcurrentHashMap<>();

    @Override
    public CommandParser<? extends Command> getCommandParser(CommandName command) {
        return commands.get(command);
    }

    @Override
    public <T extends Command> void addCommandParser(CommandName command, CommandParser<T> parser) {
        commands.put(command, parser);
    }

    @Override
    public CommandParser<? extends Command> removeCommandParser(CommandName command) {
        return commands.remove(command);
    }

    @Override
    public ModuleParser<? extends Module> getModuleParser(String moduleName, int moduleVersion) {
        return modules.get(ModuleKey.key(moduleName, moduleVersion));
    }

    @Override
    public <T extends Module> void addModuleParser(String moduleName, int moduleVersion, ModuleParser<T> parser) {
        modules.put(ModuleKey.key(moduleName, moduleVersion), parser);
    }

    @Override
    public ModuleParser<? extends Module> removeModuleParser(String moduleName, int moduleVersion) {
        return modules.remove(ModuleKey.key(moduleName, moduleVersion));
    }

    public void submitEvent(Event event) {
        try {
            if (event instanceof KeyValuePair<?>) {
                doRdbListener(this, (KeyValuePair<?>) event);
            } else if (event instanceof Command) {
                doCommandListener(this, (Command) event);
            } else if (event instanceof PreFullSyncEvent) {
                doPreFullSync(this);
            } else if (event instanceof PostFullSyncEvent) {
                doPostFullSync(this, ((PostFullSyncEvent) event).getChecksum());
            } else if (event instanceof AuxField) {
                doAuxFieldListener(this, (AuxField) event);
            }
        } catch (UncheckedIOException e) {
            throw e;
            //ignore UncheckedIOException so that to propagate to caller.
        } catch (Throwable e) {
            doExceptionListener(this, e, event);
        }
    }

    @Override
    public boolean verbose() {
        return configuration != null && configuration.isVerbose();
    }

    @Override
    public Status getStatus() {
        return connected.get();
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void setRdbVisitor(RdbVisitor rdbVisitor) {
        this.rdbVisitor = rdbVisitor;
    }

    @Override
    public RdbVisitor getRdbVisitor() {
        return this.rdbVisitor;
    }

    @Override
    public void builtInCommandParserRegister() {
        addCommandParser(CommandName.name("PING"), new PingParser());
        addCommandParser(CommandName.name("APPEND"), new AppendParser());
        addCommandParser(CommandName.name("SET"), new SetParser());
        addCommandParser(CommandName.name("SETEX"), new SetExParser());
        addCommandParser(CommandName.name("MSET"), new MSetParser());
        addCommandParser(CommandName.name("DEL"), new DelParser());
        addCommandParser(CommandName.name("SADD"), new SAddParser());
        addCommandParser(CommandName.name("HMSET"), new HMSetParser());
        addCommandParser(CommandName.name("HSET"), new HSetParser());
        addCommandParser(CommandName.name("LSET"), new LSetParser());
        addCommandParser(CommandName.name("EXPIRE"), new ExpireParser());
        addCommandParser(CommandName.name("EXPIREAT"), new ExpireAtParser());
        addCommandParser(CommandName.name("GETSET"), new GetSetParser());
        addCommandParser(CommandName.name("HSETNX"), new HSetNxParser());
        addCommandParser(CommandName.name("MSETNX"), new MSetNxParser());
        addCommandParser(CommandName.name("PSETEX"), new PSetExParser());
        addCommandParser(CommandName.name("SETNX"), new SetNxParser());
        addCommandParser(CommandName.name("SETRANGE"), new SetRangeParser());
        addCommandParser(CommandName.name("HDEL"), new HDelParser());
        addCommandParser(CommandName.name("LPOP"), new LPopParser());
        addCommandParser(CommandName.name("LPUSH"), new LPushParser());
        addCommandParser(CommandName.name("LPUSHX"), new LPushXParser());
        addCommandParser(CommandName.name("LRem"), new LRemParser());
        addCommandParser(CommandName.name("RPOP"), new RPopParser());
        addCommandParser(CommandName.name("RPUSH"), new RPushParser());
        addCommandParser(CommandName.name("RPUSHX"), new RPushXParser());
        addCommandParser(CommandName.name("ZREM"), new ZRemParser());
        addCommandParser(CommandName.name("RENAME"), new RenameParser());
        addCommandParser(CommandName.name("INCR"), new IncrParser());
        addCommandParser(CommandName.name("DECR"), new DecrParser());
        addCommandParser(CommandName.name("INCRBY"), new IncrByParser());
        addCommandParser(CommandName.name("DECRBY"), new DecrByParser());
        addCommandParser(CommandName.name("PERSIST"), new PersistParser());
        addCommandParser(CommandName.name("SELECT"), new SelectParser());
        addCommandParser(CommandName.name("FLUSHALL"), new FlushAllParser());
        addCommandParser(CommandName.name("FLUSHDB"), new FlushDBParser());
        addCommandParser(CommandName.name("HINCRBY"), new HIncrByParser());
        addCommandParser(CommandName.name("ZINCRBY"), new ZIncrByParser());
        addCommandParser(CommandName.name("MOVE"), new MoveParser());
        addCommandParser(CommandName.name("SMOVE"), new SMoveParser());
        addCommandParser(CommandName.name("PFADD"), new PFAddParser());
        addCommandParser(CommandName.name("PFCOUNT"), new PFCountParser());
        addCommandParser(CommandName.name("PFMERGE"), new PFMergeParser());
        addCommandParser(CommandName.name("SDIFFSTORE"), new SDiffStoreParser());
        addCommandParser(CommandName.name("SINTERSTORE"), new SInterStoreParser());
        addCommandParser(CommandName.name("SUNIONSTORE"), new SUnionStoreParser());
        addCommandParser(CommandName.name("ZADD"), new ZAddParser());
        addCommandParser(CommandName.name("ZINTERSTORE"), new ZInterStoreParser());
        addCommandParser(CommandName.name("ZUNIONSTORE"), new ZUnionStoreParser());
        addCommandParser(CommandName.name("BRPOPLPUSH"), new BRPopLPushParser());
        addCommandParser(CommandName.name("LINSERT"), new LInsertParser());
        addCommandParser(CommandName.name("RENAMENX"), new RenameNxParser());
        addCommandParser(CommandName.name("RESTORE"), new RestoreParser());
        addCommandParser(CommandName.name("PEXPIRE"), new PExpireParser());
        addCommandParser(CommandName.name("PEXPIREAT"), new PExpireAtParser());
        addCommandParser(CommandName.name("GEOADD"), new GeoAddParser());
        addCommandParser(CommandName.name("EVAL"), new EvalParser());
        addCommandParser(CommandName.name("SCRIPT"), new ScriptParser());
        addCommandParser(CommandName.name("PUBLISH"), new PublishParser());
        addCommandParser(CommandName.name("BITOP"), new BitOpParser());
        addCommandParser(CommandName.name("BITFIELD"), new BitFieldParser());
        addCommandParser(CommandName.name("SETBIT"), new SetBitParser());
        addCommandParser(CommandName.name("SREM"), new SRemParser());
        addCommandParser(CommandName.name("UNLINK"), new UnLinkParser());
        addCommandParser(CommandName.name("SWAPDB"), new SwapDBParser());
        addCommandParser(CommandName.name("MULTI"), new MultiParser());
        addCommandParser(CommandName.name("EXEC"), new ExecParser());
        addCommandParser(CommandName.name("ZREMRANGEBYSCORE"), new ZRemRangeByScoreParser());
        addCommandParser(CommandName.name("ZREMRANGEBYRANK"), new ZRemRangeByRankParser());
        addCommandParser(CommandName.name("ZREMRANGEBYLEX"), new ZRemRangeByLexParser());
        addCommandParser(CommandName.name("LTRIM"), new LTrimParser());
        addCommandParser(CommandName.name("SORT"), new SortParser());
        addCommandParser(CommandName.name("RPOPLPUSH"), new RPopLPushParser());
    }

    @Override
    public void close() throws IOException {
        this.connected.compareAndSet(CONNECTED, DISCONNECTING);
    }

    protected void doClose() throws IOException {
        this.connected.compareAndSet(CONNECTED, DISCONNECTING);
        try {
            if (inputStream != null) {
                this.inputStream.setRawByteListeners(null);
                inputStream.close();
            }
        } catch (IOException ignore) {
            /*NOP*/
        } finally {
            this.connected.set(DISCONNECTED);
        }
    }
}
