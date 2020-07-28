package org.apache.rocketmq.connect.runtime.tools.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.rocketmq.tools.command.SubCommandException;

public class CreateConnectorSubCommand implements SubCommand {
    @Override
    public String commandName() {
        return "createConnector";
    }

    @Override
    public String commandDesc() {
        return "Create and start a connector by connector's config";
    }

    @Override
    public Options buildCommandlineOptions(Options var1) {
        return null;
    }

    @Override
    public void execute(CommandLine var1, Options var2) throws SubCommandException {

    }
}
