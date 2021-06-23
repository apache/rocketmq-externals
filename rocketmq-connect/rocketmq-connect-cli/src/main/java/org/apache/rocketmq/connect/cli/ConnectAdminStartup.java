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

package org.apache.rocketmq.connect.cli;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.rocketmq.common.MQVersion;
import org.apache.rocketmq.connect.cli.command.SubCommand;
import org.apache.rocketmq.connect.cli.command.GetAllocatedConnectors;
import org.apache.rocketmq.connect.cli.command.GetAllocatedTasks;
import org.apache.rocketmq.connect.cli.command.CreateConnectorSubCommand;
import org.apache.rocketmq.connect.cli.command.GetAllocatedInfoCommand;
import org.apache.rocketmq.connect.cli.command.GetClusterInfoSubCommand;
import org.apache.rocketmq.connect.cli.command.GetConfigInfoSubCommand;
import org.apache.rocketmq.connect.cli.command.QueryConnectorConfigSubCommand;
import org.apache.rocketmq.connect.cli.command.QueryConnectorStatusSubCommand;
import org.apache.rocketmq.connect.cli.command.ReloadPluginsSubCommand;
import org.apache.rocketmq.connect.cli.command.StopAllSubCommand;
import org.apache.rocketmq.connect.cli.command.StopConnectorSubCommand;
import org.apache.rocketmq.connect.cli.commom.Config;
import org.apache.rocketmq.connect.cli.utils.FileAndPropertyUtil;
import org.apache.rocketmq.connect.cli.utils.ServerUtil;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;


import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ConnectAdminStartup {

    protected static List<SubCommand> subCommandList = new ArrayList<SubCommand>();

    public static CommandLine commandLine = null;

    public static String configFile = null;

    public static Properties properties = null;

    public static void main(String[] args) {
        System.setProperty(RemotingCommand.REMOTING_VERSION_KEY, Integer.toString(MQVersion.CURRENT_VERSION));

        String[] configArgs = Arrays.copyOfRange(args, 0, 2);
        args = Arrays.copyOfRange(args, 2, args.length);

        try {

            // Build the command line options.
            Options option = ServerUtil.buildCommandlineOptions(new Options());
            commandLine = ServerUtil.parseCmdLine("connect", configArgs, buildCommandlineOptions(option),
                    new PosixParser());
            if (null == commandLine) {
                System.exit(-1);
            }

            // Load configs from command line.
            Config config = new Config();
            if (commandLine.hasOption('s')) {
                String file = commandLine.getOptionValue('s');
                if (file != null) {
                    configFile = file;
                    InputStream in = new BufferedInputStream(new FileInputStream(file));
                    properties = new Properties();
                    properties.load(in);
                    FileAndPropertyUtil.properties2Object(properties, config);
                    in.close();
                }
            }

            initCommand(config);

            switch (args.length) {
                case 0:
                    printHelp();
                    break;
                case 2:
                    if (args[0].equals("help")) {
                        SubCommand cmd = findSubCommand(args[1]);
                        if (cmd != null) {
                            Options options = ServerUtil.buildCommandlineOptions(new Options());
                            options = cmd.buildCommandlineOptions(options);
                            if (options != null) {
                                ServerUtil.printCommandLineHelp("connectAdmin " + cmd.commandName(), options);
                            }
                        } else {
                            System.out.printf("The sub command %s not exist.%n", args[1]);
                        }
                        break;
                    }
                case 1:
                default:
                    SubCommand cmd = findSubCommand(args[0]);
                    if (cmd != null) {
                        String[] subargs = parseSubArgs(args);
                        Options options = ServerUtil.buildCommandlineOptions(new Options());
                        final CommandLine commandLine =
                                ServerUtil.parseCmdLine("connectAdmin " + cmd.commandName(), subargs, cmd.buildCommandlineOptions(options),
                                        new PosixParser());
                        if (null == commandLine) {
                            return;
                        }

                        cmd.execute(commandLine, options);
                    } else {
                        System.out.printf("The sub command %s not exist.%n", args[0]);
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initCommand(Config config) {
        initCommand(new CreateConnectorSubCommand(config));
        initCommand(new StopConnectorSubCommand(config));
        initCommand(new QueryConnectorConfigSubCommand(config));
        initCommand(new QueryConnectorStatusSubCommand(config));
        initCommand(new StopAllSubCommand(config));
        initCommand(new ReloadPluginsSubCommand(config));
        initCommand(new GetConfigInfoSubCommand(config));
        initCommand(new GetClusterInfoSubCommand(config));
        initCommand(new GetAllocatedInfoCommand(config));
        initCommand(new GetAllocatedConnectors(config));
        initCommand(new GetAllocatedTasks(config));
    }


    private static void printHelp() {
        System.out.printf("The most commonly used connectAdmin commands are:%n");
        for (SubCommand cmd : subCommandList) {
            System.out.printf("   %-25s %s%n", cmd.commandName(), cmd.commandDesc());
        }

        System.out.printf("%nSee 'connectAdmin help <command>' for more information on a specific command.%n");
    }

    private static SubCommand findSubCommand(final String name) {
        for (SubCommand cmd : subCommandList) {
            if (cmd.commandName().toUpperCase().equals(name.toUpperCase())) {
                return cmd;
            }
        }

        return null;
    }

    private static String[] parseSubArgs(String[] args) {
        if (args.length > 1) {
            String[] result = new String[args.length - 1];
            for (int i = 0; i < args.length - 1; i++) {
                result[i] = args[i + 1];
            }
            return result;
        }
        return null;
    }

    public static void initCommand(SubCommand command) {
        subCommandList.add(command);
    }

    private static Options buildCommandlineOptions(Options options) {

        Option opt = new Option("s", "configFile", true, "connect config properties file");
        opt.setRequired(false);
        options.addOption(opt);

        return options;
    }

}
