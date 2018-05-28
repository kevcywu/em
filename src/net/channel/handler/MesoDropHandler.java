package net.channel.handler;

import client.MapleClient;
import net.AbstractMaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;

public class MesoDropHandler extends AbstractMaplePacketHandler {

    /**
     * Creates a new instance of MesoDropHandler
     */
    public MesoDropHandler() {
    }

    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readInt(); // i don't know :)
        int meso = slea.readInt();
        if (meso < 10 || meso > 50000) {
            // AutobanManager.getInstance().addPoints(c, 1000, 0, "Dropping " + meso + " mesos");
            return;
        }
        if (meso <= c.getPlayer().getMeso()) {
            c.getPlayer().gainMeso(-meso, true, true);
            c.getPlayer().getMap().spawnMesoDrop(meso, meso, c.getPlayer().getPosition(), c.getPlayer(),
                    c.getPlayer(), false);
        }
    }
}
