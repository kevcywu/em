package client.messages.commands;

import static client.messages.CommandProcessor.getOptionalIntArg;
import client.MapleClient;
import client.messages.Command;
import client.messages.CommandDefinition;
import client.messages.IllegalCommandSyntaxException;
import client.messages.MessageCallback;
import tools.MaplePacketCreator;

public class TestCommands implements Command {

    @Override
    public void execute(MapleClient c, MessageCallback mc, String[] splitted) throws Exception,
            IllegalCommandSyntaxException {
        if (splitted[0].equals("!test")) {
            // faeks id is 30000 (30 75 00 00)
            // MapleCharacter faek = ((MapleCharacter) c.getPlayer().getMap().getMapObject(30000));

            c.getSession().write(MaplePacketCreator.getPacketFromHexString("2B 00 14 30 C0 23 00 00 11 00 00 00"));
        } else if (splitted[0].equals("!clock")) {
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getClock(getOptionalIntArg(splitted, 1, 60)));
        }

    }

    @Override
    public CommandDefinition[] getDefinition() {
        return new CommandDefinition[]{
            new CommandDefinition("test", "?", "Probably does something", 1000),
            new CommandDefinition("clock", "[time]", "Shows a clock to everyone in the map", 1000),};
    }

}
