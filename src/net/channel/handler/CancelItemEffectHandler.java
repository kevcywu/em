package net.channel.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import tools.data.input.SeekableLittleEndianAccessor;

public class CancelItemEffectHandler extends AbstractMaplePacketHandler {

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int sourceid = slea.readInt();
        MapleStatEffect effect = MapleItemInformationProvider.getInstance().getItemEffect(-sourceid);
        c.getPlayer().cancelEffect(effect, false, -1);
    }
}
