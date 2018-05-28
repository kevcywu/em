package net.channel.handler;

import client.MapleClient;
//import client.messages.ServernoticeMapleClientMessageCallback;
import net.AbstractMaplePacketHandler;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class EnterMTSHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
//        new ServernoticeMapleClientMessageCallback(5, c).dropMessage("The MTS is not available");
        c.getSession().write(MaplePacketCreator.enableActions());
    }
}
