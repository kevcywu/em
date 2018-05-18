package client.messages.commands;

import client.MapleClient;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import client.messages.MessageCallback;

public class ArrayCommand implements Command {

    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splittedLine) throws Exception,
            IllegalCommandSyntaxException {
        mc.dropMessage("Array");
    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[]{
            new CommandDefinition("array", "", "Shows array", 0),};
    }

}
