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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.connect.runtime.tools.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.rocketmq.connect.runtime.rest.RestSender;
import org.apache.rocketmq.tools.command.SubCommandException;

public class GetAllocatedInfoCommand implements SubCommand {
    @Override
    public String commandName() {
        return "getAllocatedInfo";
    }

    @Override
    public String commandDesc() {
        return "Get the load information of the current worker";
    }

    @Override
    public Options buildCommandlineOptions(Options options) {
        return options;
    }

    @Override
    public void execute(CommandLine commandLine, Options options) throws SubCommandException {
        try {
            String url = "http://localhost:8081/" + commandName();
            System.out.println("Send request to " + url);
            String result = new RestSender().sendHttpRequest(url, "");
            System.out.println(result);
        }catch (Exception e) {
            throw new SubCommandException(this.getClass().getSimpleName() + " command failed", e);
        }
    }
}
