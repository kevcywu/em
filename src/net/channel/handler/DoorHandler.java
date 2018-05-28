package net.channel.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import server.maps.MapleDoor;
import server.maps.MapleMapObject;
import tools.data.input.SeekableLittleEndianAccessor;

public class DoorHandler extends AbstractMaplePacketHandler {

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        int oid = slea.readInt();
        @SuppressWarnings("unused")
        byte mode = slea.readByte(); // specifies if backwarp or not, but currently we do not care
        for (MapleMapObject obj : c.getPlayer().getMap().getMapObjects()) {
            if (obj instanceof MapleDoor) {
                MapleDoor door = (MapleDoor) obj;
                if (door.getOwner().getId() == oid) {
                    door.warp(c.getPlayer());
                    return;
                }
            }
        }
    }

}
