package org.apache.rocketmq.connect.runtime.tools;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.rocketmq.common.MQVersion;
import org.apache.rocketmq.connect.runtime.tools.command.*;
import org.apache.rocketmq.connect.runtime.utils.ServerUtil;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;


import java.util.ArrayList;
import java.util.List;

public class MQConnectAdminStartup {
    protected static List<SubCommand> subCommandList = new ArrayList<SubCommand>();


    public static void main(String[] args) {
        System.setProperty(RemotingCommand.REMOTING_VERSION_KEY, Integer.toString(MQVersion.CURRENT_VERSION));

        initCommand();

        try {
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
                                ServerUtil.printCommandLineHelp("mqadmin " + cmd.commandName(), options);
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
                                ServerUtil.parseCmdLine("mqadmin " + cmd.commandName(), subargs, cmd.buildCommandlineOptions(options),
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

    public static void initCommand() {
        initCommand(new CreateConnectorSubCommand());
        initCommand(new StopConnectorSubCommand());
        initCommand(new QueryConnectorConfigSubCommand());
        initCommand(new QueryConnectorStatusSubCommand());
        initCommand(new StopAllSubCommand());
        initCommand(new ReloadPluginsSubCommand());
        initCommand(new GetConfigInfoSubCommand());
        initCommand(new getClusterInfoSubCommand());
        initCommand(new getAllocatedInfoCommand());

    }


    private static void printHelp() {
        System.out.printf("The most commonly used mqadmin commands are:%n");
        for (SubCommand cmd : subCommandList) {
            System.out.printf("   %-20s %s%n", cmd.commandName(), cmd.commandDesc());
        }

        System.out.printf("%nSee 'mqadmin help <command>' for more information on a specific command.%n");
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
}
