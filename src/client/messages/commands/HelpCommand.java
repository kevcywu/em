package client.messages.commands;

import client.MapleClient;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.CommandProcessor;
import client.messages.IllegalCommandSyntaxException;
import client.messages.MessageCallback;

public class HelpCommand implements Command {

    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splittedLine) throws Exception,
            IllegalCommandSyntaxException {
        int page = CommandProcessor.getOptionalIntArg(splittedLine, 1, 1);
        CommandProcessor.getInstance().dropHelp(c.getPlayer(), mc, page);
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[]{
            new CommandDefinition("help", "[page - defaults to 1]", "Shows the help", 0),};
    }
}
