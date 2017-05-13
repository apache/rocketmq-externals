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

import org.apache.rocketmq.replicator.redis.cmd.Command;
import org.apache.rocketmq.replicator.redis.cmd.CommandName;
import org.apache.rocketmq.replicator.redis.cmd.CommandParser;
import org.apache.rocketmq.replicator.redis.rdb.RdbVisitor;
import org.apache.rocketmq.replicator.redis.rdb.datatype.Module;
import org.apache.rocketmq.replicator.redis.rdb.module.ModuleParser;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public interface Replicator extends Closeable, ReplicatorListener {
    /*
     * Command
     */
    void builtInCommandParserRegister();

    CommandParser<? extends Command> getCommandParser(CommandName command);

    <T extends Command> void addCommandParser(CommandName command, CommandParser<T> parser);

    CommandParser<? extends Command> removeCommandParser(CommandName command);

    /*
     * Module
     */
    ModuleParser<? extends Module> getModuleParser(String moduleName, int moduleVersion);

    <T extends Module> void addModuleParser(String moduleName, int moduleVersion, ModuleParser<T> parser);

    ModuleParser<? extends Module> removeModuleParser(String moduleName, int moduleVersion);

    /*
     * Rdb
     */
    void setRdbVisitor(RdbVisitor rdbVisitor);

    RdbVisitor getRdbVisitor();

    /*
     *
     */
    boolean verbose();

    void open() throws IOException;
}
