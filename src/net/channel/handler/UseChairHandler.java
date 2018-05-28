package net.channel.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.IItem;
import client.MapleClient;
import client.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class UseChairHandler extends AbstractMaplePacketHandler {

    private static final Logger log = LoggerFactory.getLogger(UseItemHandler.class);

    public UseChairHandler() {
    }

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {

        int itemId = slea.readInt();
        IItem toUse = c.getPlayer().getInventory(MapleInventoryType.SETUP).findById(itemId);

        if (toUse == null) {
            log.info("[h4x] Player {} is using an item he does not have: {}", c.getPlayer().getName(), Integer.valueOf(itemId));
        } else {
            c.getPlayer().setChair(itemId);
            c.getPlayer().getMap().broadcastMessage(c.getPlayer(), MaplePacketCreator.showChair(c.getPlayer().getId(), itemId), false);
        }

        c.getSession().write(MaplePacketCreator.enableActions());
    }
}
