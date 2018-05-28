package net.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import net.AbstractMaplePacketHandler;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class CharInfoRequestHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readShort(); //most likely two shorts rather than one int but dunno ^___^
        slea.readShort();
        int cid = slea.readInt();
        c.getSession().write(MaplePacketCreator.charInfo((MapleCharacter) c.getPlayer().getMap().getMapObject(cid)));
    }
}
