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

import java.io.Closeable;
import java.io.IOException;
import org.apache.rocketmq.redis.replicator.rdb.RdbVisitor;
import org.apache.rocketmq.redis.replicator.rdb.module.ModuleParser;
import org.apache.rocketmq.redis.replicator.cmd.Command;
import org.apache.rocketmq.redis.replicator.cmd.CommandName;
import org.apache.rocketmq.redis.replicator.cmd.CommandParser;
import org.apache.rocketmq.redis.replicator.rdb.datatype.Module;

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

    Status getStatus();

    /**
     * @return configuration
     */
    Configuration getConfiguration();

    void open() throws IOException;
}
