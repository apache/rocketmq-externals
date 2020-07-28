package org.apache.rocketmq.connect.runtime.tools.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.rocketmq.connect.runtime.rest.RestSender;
import org.apache.rocketmq.tools.command.SubCommandException;

public class QueryConnectorConfigSubCommand implements SubCommand {
    @Override
    public String commandName() {
        return "queryConnectorConfig";
    }

    @Override
    public String commandDesc() {
        return "Get configuration information for a connector";
    }

    @Override
    public Options buildCommandlineOptions(Options options) {
        Option opt = new Option("c", "connectorName", true, "connector name");
        opt.setRequired(true);
        options.addOption(opt);

        return options;
    }

    @Override
    public void execute(CommandLine commandLine, Options options) throws SubCommandException {
        try{
            String connectorName = commandLine.getOptionValue('c').trim();
            String url = "http://localhost:8081/connectors/" + connectorName + "/config";
            System.out.println("Send request to " + url);
            String result = new RestSender().sendHttpRequest(url, "");
            System.out.println(result);
        }catch (Exception e){
            throw new SubCommandException(this.getClass().getSimpleName() + " command failed", e);
        }
    }
}
