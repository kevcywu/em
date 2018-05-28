package net.channel.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.MapleInventoryManipulator;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import net.packetcreator.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class ItemPickupHandler extends AbstractMaplePacketHandler {

    private static final Logger log = LoggerFactory.getLogger(ItemPickupHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte mode = slea.readByte(); // or something like that...but better ignore it if you want
        // mapchange to work! o.o!
        slea.readInt(); //?
        slea.readInt(); // position, but we dont need it o.o
        int oid = slea.readInt();
        MapleMapObject ob = c.getPlayer().getMap().getMapObject(oid);
        if (ob == null) {
            c.getSession().write(MaplePacketCreator.getInventoryFull());
            c.getSession().write(MaplePacketCreator.getShowInventoryFull());
            return;
        }
        if (ob instanceof MapleMapItem) {
            MapleMapItem mapitem = (MapleMapItem) ob;
            synchronized (mapitem) {
                if (mapitem.isPickedUp()) {
                    c.getSession().write(MaplePacketCreator.getInventoryFull());
                    c.getSession().write(MaplePacketCreator.getShowInventoryFull());
                    return;
                }
                double distance = c.getPlayer().getPosition().distanceSq(mapitem.getPosition());
                if (distance > 90000.0) { // 300^2, 550 is approximatly the range of ultis
                    // AutobanManager.getInstance().addPoints(c, 100, 300000, "Itemvac");
                    // Double.valueOf(Math.sqrt(distance))
                } else if (distance > 22500.0) {
                    // log.warn("[h4x] Player {} is picking up an item that's fairly far away: {}", c.getPlayer().getName(), Double.valueOf(Math.sqrt(distance)));
                }
                if (mapitem.getMeso() > 0) {
                    c.getPlayer().gainMeso(mapitem.getMeso(), true, true);
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, c.getPlayer().getId()), mapitem.getPosition());
                    c.getPlayer().getMap().removeMapObject(ob);
                } else {
                    String logInfo = "Picked up by " + c.getPlayer().getName();
                    if (MapleInventoryManipulator.addFromDrop(c, mapitem.getItem(), logInfo)) {
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapitem.getObjectId(), 2, c.getPlayer().getId()), mapitem.getPosition());
                        c.getPlayer().getMap().removeMapObject(ob);
                    } else {
                        return;
                    }
                }
                mapitem.setPickedUp(true);
            }
        }
    }

}
