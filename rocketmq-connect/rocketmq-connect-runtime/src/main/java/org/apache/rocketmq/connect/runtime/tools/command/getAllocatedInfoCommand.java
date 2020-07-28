package org.apache.rocketmq.connect.runtime.tools.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.rocketmq.connect.runtime.rest.RestSender;
import org.apache.rocketmq.tools.command.SubCommandException;

public class getAllocatedInfoCommand implements SubCommand {
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
        }catch (Exception e){
            throw new SubCommandException(this.getClass().getSimpleName() + " command failed", e);
        }
    }
}
