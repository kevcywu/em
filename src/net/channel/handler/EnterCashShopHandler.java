package net.channel.handler;

import client.MapleClient;
//import client.messages.ServernoticeMapleClientMessageCallback;
import net.AbstractMaplePacketHandler;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class EnterCashShopHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
//        new ServernoticeMapleClientMessageCallback(5, c).dropMessage("The Cash Shop is not available");
        c.getSession().write(MaplePacketCreator.enableActions());
    }
}
