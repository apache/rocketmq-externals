package org.apache.rocketmq.connect.runtime.tools.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.rocketmq.connect.runtime.rest.RestSender;
import org.apache.rocketmq.tools.command.SubCommandException;

import java.io.*;
import java.nio.charset.StandardCharsets;

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
    public Options buildCommandlineOptions(Options options) {
        Option opt = new Option("c", "connectorName", true, "connector name");
        opt.setRequired(true);
        options.addOption(opt);
        opt = new Option("p", "path", true, "Configuration file path");
        opt.setRequired(true);
        options.addOption(opt);
        return options;
    }

    @Override
    public void execute(CommandLine commandLine, Options options) throws SubCommandException {
        try{
            String connectorName = commandLine.getOptionValue('c').trim();
            String baseUrl = "http://localhost:8081/connectors/" + connectorName + "?config=";
            String filePath = commandLine.getOptionValue('p').trim();
            String config = readFile(filePath);
            System.out.println("Send request to " + baseUrl + config);
            String result = new RestSender().sendHttpRequest(baseUrl, config);
            System.out.println(result);
        }catch (Exception e){
            throw new SubCommandException(this.getClass().getSimpleName() + " command failed", e);
        }
    }

    private String readFile(String filePath) {
        StringBuilder sb = new StringBuilder();
        try{
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8));
            int index = 0;
            while ((index = reader.read()) != -1) {
                sb.append((char) index);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
