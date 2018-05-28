package net.channel.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import client.IItem;
import client.MapleClient;
import client.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class UseItemHandler extends AbstractMaplePacketHandler {

    private static final Logger log = LoggerFactory.getLogger(UseItemHandler.class);

    /**
     * Creates a new instance of UseItemHandler
     */
    public UseItemHandler() {
    }

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (!c.getPlayer().isAlive()) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        slea.readInt(); // i have no idea :) (o.o)
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt(); //as if we cared... ;)
        //List<IItem> existing = c.getPlayer().getInventory(MapleInventoryType.USE).listById(itemId);
        IItem toUse = c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse != null && toUse.getQuantity() > 0) {
            if (toUse.getItemId() != itemId) {
                log.info("[h4x] Player {} is using an item not in the slot: {}", c.getPlayer().getName(), Integer.valueOf(itemId));
            }
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
            ii.getItemEffect(toUse.getItemId()).applyTo(c.getPlayer());
        } else {
            log.info("[h4x] Player {} is using an item he does not have: {}", c.getPlayer().getName(), Integer.valueOf(itemId));
        }
    }
}
