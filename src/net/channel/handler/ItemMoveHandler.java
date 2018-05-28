package net.channel.handler;

import client.MapleClient;
import client.MapleInventoryType;
import net.AbstractMaplePacketHandler;
import server.MapleInventoryManipulator;
import tools.data.input.SeekableLittleEndianAccessor;

public class ItemMoveHandler extends AbstractMaplePacketHandler {
    // private static Logger log = LoggerFactory.getLogger(ItemMoveHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readInt(); //?
        MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
        byte src = (byte) slea.readShort();
        byte dst = (byte) slea.readShort();
        short quantity = slea.readShort();
        if (src < 0 && dst > 0) {
            MapleInventoryManipulator.unequip(c, src, dst);
        } else if (dst < 0) {
            MapleInventoryManipulator.equip(c, src, dst);
        } else if (dst == 0) {
            MapleInventoryManipulator.drop(c, type, src, quantity);
        } else {
            MapleInventoryManipulator.move(c, type, src, dst);
        }
    }

}
