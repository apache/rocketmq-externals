package org.apache.rocketmq.connect.runtime.tools.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.rocketmq.tools.command.SubCommandException;

public interface SubCommand {
    String commandName();

    String commandDesc();

    Options buildCommandlineOptions(Options options);

    void execute(CommandLine commandLine, Options options)  throws SubCommandException;
}
