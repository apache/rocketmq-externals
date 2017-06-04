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

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.rocketmq.replicator.redis.cmd.Command;
import org.apache.rocketmq.replicator.redis.cmd.CommandName;
import org.apache.rocketmq.replicator.redis.cmd.CommandParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.AppendParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.BRPopLPushParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.BitFieldParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.BitOpParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.DecrByParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.DecrParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.DelParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.EvalParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.ExecParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.ExpireAtParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.ExpireParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.FlushAllParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.FlushDBParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.GeoAddParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.GetSetParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.HDelParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.HIncrByParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.HMSetParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.HSetNxParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.HSetParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.IncrByParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.IncrParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.LInsertParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.LPopParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.LPushParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.LPushXParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.LRemParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.LSetParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.MSetNxParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.MSetParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.MoveParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.MultiParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.PExpireAtParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.PExpireParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.PFAddParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.PFCountParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.PFMergeParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.PSetExParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.PersistParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.PingParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.PublishParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.RPopParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.RPushParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.RPushXParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.RenameNxParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.RenameParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.RestoreParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.SAddParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.SDiffStoreParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.SInterStoreParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.SMoveParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.SRemParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.SUnionStoreParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.ScriptParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.SelectParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.SetBitParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.SetExParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.SetNxParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.SetParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.SetRangeParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.SwapDBParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.UnLinkParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.ZAddParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.ZIncrByParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.ZInterStoreParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.ZRemParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.ZRemRangeByLexParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.ZRemRangeByRankParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.ZRemRangeByScoreParser;
import org.apache.rocketmq.replicator.redis.cmd.parser.ZUnionStoreParser;
import org.apache.rocketmq.replicator.redis.event.Event;
import org.apache.rocketmq.replicator.redis.event.PostFullSyncEvent;
import org.apache.rocketmq.replicator.redis.event.PreFullSyncEvent;
import org.apache.rocketmq.replicator.redis.io.RedisInputStream;
import org.apache.rocketmq.replicator.redis.rdb.DefaultRdbVisitor;
import org.apache.rocketmq.replicator.redis.rdb.RdbVisitor;
import org.apache.rocketmq.replicator.redis.rdb.datatype.AuxField;
import org.apache.rocketmq.replicator.redis.rdb.datatype.KeyValuePair;
import org.apache.rocketmq.replicator.redis.rdb.datatype.Module;
import org.apache.rocketmq.replicator.redis.rdb.module.ModuleKey;
import org.apache.rocketmq.replicator.redis.rdb.module.ModuleParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Leon Chen
 * @version 2.1.1
 * @since 2.1.0
 */
public abstract class AbstractReplicator extends AbstractReplicatorListener implements Replicator {
    private static final Logger logger = LoggerFactory.getLogger(AbstractReplicator.class);

    protected Configuration configuration;
    protected RedisInputStream inputStream;
    protected RdbVisitor rdbVisitor = new DefaultRdbVisitor(this);
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

    public void submitObject(Object obj) {
        if (obj instanceof Object[]) {
            if (configuration.isVerbose() && logger.isDebugEnabled())
                logger.debug(Arrays.deepToString((Object[])obj));
            Object[] command = (Object[])obj;
            CommandName cmdName = CommandName.name((String)command[0]);
            final CommandParser<? extends Command> operations;
            //if command do not register. ignore
            if ((operations = commands.get(cmdName)) == null) {
                logger.warn("command [" + cmdName + "] not register. raw command:[" + Arrays.deepToString((Object[])obj) + "]");
                return;
            }
            //do command replyParser
            Command parsedCommand = operations.parse(command);
            //submit event
            this.submitEvent(parsedCommand);
        }
        else {
            logger.info("redis reply:" + obj);
        }
        return;
    }

    public void submitEvent(Event event) {
        try {
            if (event instanceof KeyValuePair<?>) {
                doRdbListener(this, (KeyValuePair<?>)event);
            }
            else if (event instanceof Command) {
                doCommandListener(this, (Command)event);
            }
            else if (event instanceof PreFullSyncEvent) {
                doPreFullSync(this);
            }
            else if (event instanceof PostFullSyncEvent) {
                doPostFullSync(this, ((PostFullSyncEvent)event).getChecksum());
            }
            else if (event instanceof AuxField) {
                doAuxFieldListener(this, (AuxField)event);
            }
        }
        catch (Throwable e) {
            doExceptionListener(this, e, event);
        }
    }

    @Override
    public boolean verbose() {
        return configuration != null && configuration.isVerbose();
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
    }

    protected void doClose() throws IOException {
        if (inputStream != null)
            try {
                this.inputStream.removeRawByteListener(this);
                inputStream.close();
            }
            catch (IOException ignore) { /*NOP*/ }
        doCloseListener(this);
    }
}
