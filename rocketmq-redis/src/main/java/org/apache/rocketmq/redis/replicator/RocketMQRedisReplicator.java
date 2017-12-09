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
import java.net.URISyntaxException;
import java.util.Objects;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.rocketmq.redis.replicator.cmd.Command;
import org.apache.rocketmq.redis.replicator.cmd.CommandListener;
import org.apache.rocketmq.redis.replicator.cmd.CommandName;
import org.apache.rocketmq.redis.replicator.cmd.CommandParser;
import org.apache.rocketmq.redis.replicator.conf.Configure;
import org.apache.rocketmq.redis.replicator.event.PostFullSyncEvent;
import org.apache.rocketmq.redis.replicator.event.PreFullSyncEvent;
import org.apache.rocketmq.redis.replicator.io.RawByteListener;
import org.apache.rocketmq.redis.replicator.producer.RocketMQProducer;
import org.apache.rocketmq.redis.replicator.rdb.RdbListener;
import org.apache.rocketmq.redis.replicator.rdb.RdbVisitor;
import org.apache.rocketmq.redis.replicator.rdb.datatype.AuxField;
import org.apache.rocketmq.redis.replicator.rdb.datatype.KeyValuePair;
import org.apache.rocketmq.redis.replicator.rdb.datatype.Module;
import org.apache.rocketmq.redis.replicator.rdb.module.ModuleParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.curator.framework.CuratorFrameworkFactory.newClient;
import static org.apache.rocketmq.redis.replicator.conf.ReplicatorConstants.CONFIG_PROP_ZK_ADDRESS;
import static org.apache.rocketmq.redis.replicator.conf.ReplicatorConstants.DEPLOY_MODEL;
import static org.apache.rocketmq.redis.replicator.conf.ReplicatorConstants.DEPLOY_MODEL_SINGLE;
import static org.apache.rocketmq.redis.replicator.conf.ReplicatorConstants.REDIS_URI;
import static org.apache.rocketmq.redis.replicator.conf.ReplicatorConstants.ROOT_DIR;

public class RocketMQRedisReplicator extends AbstractReplicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RocketMQRedisReplicator.class);

    protected final RedisURI uri;
    protected final Configure configure;
    protected final Replicator replicator;

    public RocketMQRedisReplicator(Configure configure) throws IOException, URISyntaxException {
        Objects.requireNonNull(configure);
        String uri = configure.getString(REDIS_URI);
        this.configure = configure;
        this.uri = new RedisURI(uri);
        this.replicator = new RedisReplicator(this.uri);
    }

    @Override
    public boolean addRdbListener(RdbListener listener) {
        return replicator.addRdbListener(listener);
    }

    @Override
    public boolean removeRdbListener(RdbListener listener) {
        return replicator.removeRdbListener(listener);
    }

    @Override
    public boolean addRawByteListener(RawByteListener listener) {
        return replicator.addRawByteListener(listener);
    }

    @Override
    public boolean removeRawByteListener(RawByteListener listener) {
        return replicator.removeRawByteListener(listener);
    }

    @Override
    public void builtInCommandParserRegister() {
        replicator.builtInCommandParserRegister();
    }

    @Override
    public CommandParser<? extends Command> getCommandParser(CommandName command) {
        return replicator.getCommandParser(command);
    }

    @Override
    public <T extends Command> void addCommandParser(CommandName command, CommandParser<T> parser) {
        replicator.addCommandParser(command, parser);
    }

    @Override
    public CommandParser<? extends Command> removeCommandParser(CommandName command) {
        return replicator.removeCommandParser(command);
    }

    @Override
    public ModuleParser<? extends Module> getModuleParser(String moduleName, int moduleVersion) {
        return replicator.getModuleParser(moduleName, moduleVersion);
    }

    @Override
    public <T extends Module> void addModuleParser(String moduleName, int moduleVersion, ModuleParser<T> parser) {
        replicator.addModuleParser(moduleName, moduleVersion, parser);
    }

    @Override
    public ModuleParser<? extends Module> removeModuleParser(String moduleName, int moduleVersion) {
        return replicator.removeModuleParser(moduleName, moduleVersion);
    }

    @Override
    public void setRdbVisitor(RdbVisitor rdbVisitor) {
        replicator.setRdbVisitor(rdbVisitor);
    }

    @Override
    public RdbVisitor getRdbVisitor() {
        return replicator.getRdbVisitor();
    }

    @Override
    public boolean addCommandListener(CommandListener listener) {
        return replicator.addCommandListener(listener);
    }

    @Override
    public boolean removeCommandListener(CommandListener listener) {
        return replicator.removeCommandListener(listener);
    }

    @Override
    public boolean addCloseListener(CloseListener listener) {
        return replicator.addCloseListener(listener);
    }

    @Override
    public boolean removeCloseListener(CloseListener listener) {
        return replicator.removeCloseListener(listener);
    }

    @Override
    public boolean addExceptionListener(ExceptionListener listener) {
        return replicator.addExceptionListener(listener);
    }

    @Override
    public boolean removeExceptionListener(ExceptionListener listener) {
        return replicator.removeExceptionListener(listener);
    }

    @Override
    public boolean verbose() {
        return replicator.verbose();
    }

    @Override
    public Configuration getConfiguration() {
        return replicator.getConfiguration();
    }

    @Override
    public void open() throws IOException {
        if (this.replicator instanceof RedisSocketReplicator) {
            boolean single = configure.getString(DEPLOY_MODEL).equals(DEPLOY_MODEL_SINGLE);
            if (single) {
                this.replicator.open();
            } else {
                String address = configure.getString(CONFIG_PROP_ZK_ADDRESS);
                final CuratorFramework client = newClient(address, new ExponentialBackoffRetry(1000, 3));
                client.start();

                String path = ROOT_DIR + "/" + uri.getHost() + "-" + uri.getPort();
                final LeaderSelector selector = new LeaderSelector(client, path, new LeaderSelectorListenerAdapter() {
                    @Override
                    public void takeLeadership(final CuratorFramework client) throws Exception {
                        RocketMQRedisReplicator.this.addCloseListener(new CloseListener() {
                            @Override
                            public void handle(Replicator replicator) {
                                client.close();
                            }
                        });
                        replicator.open();
                    }
                });
                selector.start();
            }
        } else {
            this.replicator.open();
        }
    }

    @Override
    public void close() throws IOException {
        replicator.close();
    }

    public static void main(String[] args) throws Exception {
        Configure configure = new Configure();
        Replicator replicator = new RocketMQRedisReplicator(configure);
        final RocketMQProducer producer = new RocketMQProducer(configure);

        replicator.addRdbListener(new RdbListener() {
            @Override public void preFullSync(Replicator replicator) {
                try {
                    if (!producer.send(new PreFullSyncEvent())) {
                        LOGGER.error("Fail to send PreFullSync event");
                    }
                } catch (Exception e) {
                    LOGGER.error("Fail to send PreFullSync event", e);
                }
            }

            @Override public void auxField(Replicator replicator, AuxField auxField) {
                try {
                    if (!producer.send(auxField)) {
                        LOGGER.error("Fail to send AuxField[{}]", auxField);
                    }
                } catch (Exception e) {
                    LOGGER.error(String.format("Fail to send AuxField[%s]", auxField), e);
                }
            }

            @Override public void handle(Replicator replicator, KeyValuePair<?> kv) {
                try {
                    if (!producer.send(kv)) {
                        LOGGER.error("Fail to send KeyValuePair[key={}]", kv.getKey());
                    }
                } catch (Exception e) {
                    LOGGER.error(String.format("Fail to send KeyValuePair[key=%s]", kv.getKey()), e);
                }
            }

            @Override public void postFullSync(Replicator replicator, long checksum) {
                try {
                    if (!producer.send(new PostFullSyncEvent(checksum))) {
                        LOGGER.error("Fail to send send PostFullSync event");
                    }
                } catch (Exception e) {
                    LOGGER.error("Fail to send PostFullSync event", e);
                }
            }
        });

        replicator.addCommandListener(new CommandListener() {
            @Override public void handle(Replicator replicator, Command command) {
                try {
                    if (!producer.send(command)) {
                        LOGGER.error("Fail to send command[{}]", command);
                    }
                } catch (Exception e) {
                    LOGGER.error(String.format("Fail to send command[%s]", command), e);
                }
            }
        });

        replicator.open();
    }
}
