package net.channel.handler;

import client.MapleClient;
import client.commands.CommandProcessor;
import net.AbstractMaplePacketHandler;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class GeneralchatHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        String text = slea.readMapleAsciiString();
        if (text.charAt(0) == '!') {
            String[] sp = text.toLowerCase().split(" ");
            CommandProcessor.executeCommand(c, sp);
        } else {
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getChatText(c.getPlayer().getId(), text));
        }
    }
}
