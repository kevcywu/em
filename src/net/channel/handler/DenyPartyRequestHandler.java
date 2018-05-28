package net.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class DenyPartyRequestHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readByte();
        String from = slea.readMapleAsciiString();
        @SuppressWarnings("unused")
        String to = slea.readMapleAsciiString(); //wtf?

        MapleCharacter cfrom = c.getChannelServer().getPlayerStorage().getCharacterByName(from);
        if (cfrom != null) {
            cfrom.getClient().getSession().write(MaplePacketCreator.partyStatusMessage(22, c.getPlayer().getName()));
        }
    }
}
