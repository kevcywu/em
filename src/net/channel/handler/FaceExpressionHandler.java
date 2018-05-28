package net.channel.handler;

import client.MapleClient;
import client.MapleInventory;
import client.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import server.MapleItemInformationProvider;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FaceExpressionHandler extends AbstractMaplePacketHandler {

    private static Logger log = LoggerFactory.getLogger(FaceExpressionHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int emote = slea.readInt();
        if (emote > 7) {
            int emoteid = 5159992 + emote;
            MapleInventoryType type = MapleItemInformationProvider.getInstance().getInventoryType(emoteid);
            MapleInventory iv = c.getPlayer().getInventory(type);
        }
        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.facialExpression(c.getPlayer(), emote), false);
    }
}
